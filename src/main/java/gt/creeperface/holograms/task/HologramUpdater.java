package gt.creeperface.holograms.task;

import cn.nukkit.InterruptibleThread;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.*;
import cn.nukkit.utils.Binary;
import cn.nukkit.utils.MainLogger;
import cn.nukkit.utils.Zlib;
import com.google.common.collect.Lists;
import gt.creeperface.holograms.Hologram;
import gt.creeperface.holograms.Hologram.EntityEntry;
import gt.creeperface.holograms.HologramConfiguration;
import gt.creeperface.holograms.Holograms;
import gt.creeperface.holograms.entity.data.EntityData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author CreeperFace
 */

public class HologramUpdater extends Thread implements InterruptibleThread {

    private final Holograms plugin;

    private final Deque<UpdateEntry> updateQueue = new ArrayDeque<>();
    private final Deque<MoveEntry> moveQueue = new ArrayDeque<>();

    private static final EntityData DEFAULT_DATA = (EntityData) new EntityData()
            .putLong(Entity.DATA_FLAGS, (
                    (1L << Entity.DATA_FLAG_CAN_SHOW_NAMETAG) |
                            (1L << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG) |
                            (1L << Entity.DATA_FLAG_IMMOBILE) |
                            (1L << Entity.DATA_FLAG_SILENT)
//                            (1L << Entity.DATA_FLAG_INVISIBLE)
            ))
            .putFloat(Entity.DATA_BOUNDING_BOX_HEIGHT, 0)
            .putFloat(Entity.DATA_BOUNDING_BOX_WIDTH, 0)
            .putFloat(Entity.DATA_SCALE, 0f)
//            .putFloat(Entity.DATA_HEALTH, 100)
            .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
            .putByte(Entity.DATA_ALWAYS_SHOW_NAMETAG, 1);

    private static final Attribute[] DEFAULT_ATTRIBUTES = new Attribute[]{/*Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(100).setValue(100)*/};

    public HologramUpdater(Holograms plugin) {
        this.plugin = plugin;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (Server.getInstance().isRunning()) {

            try {
                while (true) {
                    UpdateEntry entry;

                    synchronized (updateQueue) {
                        if (updateQueue.isEmpty()) {
                            break;
                        }

                        entry = updateQueue.poll();
                    }

                    try {
                        spawnHologram(entry);
                    } catch (Exception e) {
                        MainLogger.getLogger().critical("Could not process hologram spawn request", e);
                    }
                }

                while (true) {
                    MoveEntry entry;

                    synchronized (moveQueue) {
                        if (moveQueue.isEmpty()) {
                            break;
                        }

                        entry = moveQueue.poll();
                    }

                    List<MoveEntityPacket> movePackets = new ArrayList<>();
                    Vector3 pos = entry.pos;
                    double baseY = 0;

                    List<RemoveEntityPacket> reps = entry.entityEntry.getRemovePackets();

                    for (RemoveEntityPacket rep : reps) {
                        MoveEntityPacket pk = new MoveEntityPacket();
                        pk.eid = rep.eid;
                        pk.x = (float) pos.x;
                        pk.y = (float) (pos.y + baseY);
                        pk.z = (float) pos.z;
                        pk.onGround = true;
                        //pk.mode = 1;

                        pk.encode();
                        pk.isEncoded = true;

                        movePackets.add(pk);

                        baseY += HologramConfiguration.getLineGap();
                    }

                    MoveEntityPacket[] packets = movePackets.toArray(new MoveEntityPacket[0]);
                    Player[] players = entry.players.toArray(new Player[0]);

//                    for (MovePlayerPacket pk : packets) {
//                        MainLogger.getLogger().info("X: " + pk.x + "  Y: " + pk.y + "   Z:" + pk.z);
//                    }

                    this.sendPackets(packets, players);
                }
            } catch (Throwable t) {
                MainLogger.getLogger().error("Error occurred during updating holograms", t);
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    private void spawnHologram(UpdateEntry entry) {
        if (entry.getPlayers().size() <= 0) {
            return;
        }

        if (entry.isVisitorSensitive()/* || entry.getPlayers().size() == 1*/) {
            spawnToSeparatePlayers(entry.getPlayers(), entry);
        } else {
            spawnHologramToAll(entry);
        }
    }

    private void spawnHologramToAll(UpdateEntry updateEntry) {
        List<List<String>> trans = addPlaceHolders(updateEntry.translations, updateEntry.getPlaceholders());

        if (trans.isEmpty()) {
            return;
        }

        //MainLogger.getLogger().info("origin size: "+updateEntry.translations.size()+"   after length: "+trans.size());

        /*if (trans.equals(updateEntry.getOldLines())) {
            return;
        }*/

        List<DataPacket> removePacketsToSend = new ArrayList<>();
        List<List<DataPacket>> packetsToSend = new ArrayList<>();

        int entityCount = 0;
        for (EntityEntry entityEntry : updateEntry.entityEntries) {
            Vector3 pos = entityEntry.getSafePos();
            long[] reservedIds = updateEntry.entityIds[entityCount++];

            List<List<AddEntityPacket>> cachedPackets = new ArrayList<>();
            List<RemoveEntityPacket> cachedRemovePackets = new ArrayList<>();

            if (updateEntry.spawn)
                removePacketsToSend.addAll(entityEntry.getRemovePackets());

            if (updateEntry.spawn) {
                for (List<String> lines : trans) {
                    cachedPackets.add(compile(lines, pos, reservedIds, true));
                }
            } else {
                int transIndex = 0;

                //MainLogger.getLogger().info("trans size: "+trans.size());
                for (List<String> lines : trans) {
                    List<DataPacket> setData = new ArrayList<>();

                    int j = 0;
                    for (String line : lines) {
                        SetEntityDataPacket pk = new SetEntityDataPacket();
                        pk.eid = reservedIds[j++];
                        pk.metadata = new EntityMetadata()
                                .putString(Entity.DATA_NAMETAG, line);


                        pk.encode();
                        pk.isEncoded = true;

                        setData.add(pk);
                    }

                    List<DataPacket> langPackets;
                    if (transIndex >= packetsToSend.size()) {
                        langPackets = new ArrayList<>();
                        packetsToSend.add(langPackets);
                    } else {
                        langPackets = packetsToSend.get(transIndex);
                    }

                    langPackets.addAll(setData);
                    //MainLogger.getLogger().info("index: "+transIndex);
                    transIndex++;
                }
            }

            if (cachedPackets.size() > 0 && (updateEntry.isRecache() || entityEntry.getRemovePackets().isEmpty())) { //convert IDs
                /*List<AddPlayerPacket> first = cachedPackets.get(0);
                for (AddPlayerPacket pk : first) {
                    RemoveEntityPacket rpk = new RemoveEntityPacket();
                    rpk.eid = pk.entityRuntimeId;

                    rpk.encode();
                    rpk.isEncoded = true;

                    cachedRemovePackets.add(rpk);
                }

                for (int i = 1; i < cachedPackets.size(); i++) {
                    List<AddPlayerPacket> replaces = cachedPackets.get(i);

                    for (int j = 0; j < first.size(); j++) {
                        AddPlayerPacket replace = replaces.get(j);
                        replace.entityRuntimeId = first.get(j).entityRuntimeId;
                        replace.entityUniqueId = first.get(j).entityUniqueId;
                    }
                }

                for (List<AddPlayerPacket> apks : cachedPackets) {
                    packetsToSend.add((List) apks);
                }*/
                for (long id : reservedIds) {
                    RemoveEntityPacket rpk = new RemoveEntityPacket();
                    rpk.eid = id;

                    rpk.encode();
                    rpk.isEncoded = true;

                    cachedRemovePackets.add(rpk);
                }

                entityEntry.cachePackets(cachedPackets);

                Collections.reverse(cachedRemovePackets);
                entityEntry.cacheRemovedPackets(cachedRemovePackets);
            }

            packetsToSend.addAll((List) cachedPackets);
        }

        List<PacketsEntry> packetsEntries = new ArrayList<>();

        //MainLogger.getLogger().info("size0: "+packetsToSend.size());
        for (int i = 0; i < packetsToSend.size(); i++) {
            PacketsEntry entry = packetsEntries.size() > i ? packetsEntries.get(i) : null;
            if (entry == null) {
                entry = new PacketsEntry();
                packetsEntries.add(entry);
            }

            if (updateEntry.spawn) {
                entry.packets.addAll(removePacketsToSend);
            }
            entry.packets.addAll(packetsToSend.get(i));

            for (PlayerEntry playerEntry : updateEntry.getPlayers()) {

                if (playerEntry.language == i) {
                    entry.players.add(playerEntry.player);
                }
            }
        }

        /*StringBuilder builder = new StringBuilder();
        builder.append("Hologram data bump: \n\n");

        MainLogger.getLogger().info("hologram size: "+packetsEntries.size());
        int i = 0;
        for(PacketsEntry pe : packetsEntries) {
            builder.append("trans: ");
            builder.append(i);
            builder.append(" {\n");

            builder.append("    players {\n");
            for(Player p : pe.players) {
                builder.append("        ");
                builder.append(p.getName());
                builder.append("\n");
            }
            builder.append("    }\n");

            builder.append("\n    lines {\n");
            for(DataPacket pk : pe.packets) {
                if(pk instanceof AddPlayerPacket) {
                    AddPlayerPacket apk = (AddPlayerPacket) pk;

                    builder.append("        APK ID: ");
                    builder.append(apk.entityRuntimeId);
                    builder.append("\n");
                    builder.append(apk.username);
                    builder.append("\n");
                } else if(pk instanceof SetEntityDataPacket) {
                    SetEntityDataPacket sedp = (SetEntityDataPacket) pk;

                    builder.append("        SEDP ID: ");
                    builder.append(sedp.eid);
                    builder.append("\n");
                    builder.append(sedp.metadata.getString(Entity.DATA_NAMETAG));
                    builder.append("\n");
                }
            }
            builder.append("    }\n");

            builder.append("}\n");
            i++;
        }

        MainLogger.getLogger().info(builder.toString());*/

        for (PacketsEntry packetsEntry : packetsEntries) {
            this.sendPackets(packetsEntry.packets.toArray(new DataPacket[0]), packetsEntry.players.toArray(new Player[0]));
        }
    }

    private void spawnToSeparatePlayers(Collection<PlayerEntry> playerEntries, UpdateEntry updateEntry) {
        List<SinglePlayerEntry> packets = new ArrayList<>();
        Collection<EntityEntry> entityEntries = updateEntry.entityEntries;

        if (playerEntries.size() <= 0) {
            return;
        }

        List<List<String>> trans = addPlaceHolders(updateEntry.translations, updateEntry.getPlaceholders());
        updateEntry.translations.clear();
        updateEntry.translations.addAll(trans);


        for (PlayerEntry entry : playerEntries) {
            packets.add(spawnHologramTo(entry, updateEntry, entityEntries));
        }

        for (EntityPacketEntry packetEntry : packets.get(0).packetEntries) {
            EntityEntry entityEntry = packetEntry.entityEntry;

            List<RemoveEntityPacket> removePackets = entityEntry.getRemovePackets();

            if (updateEntry.spawn || removePackets.isEmpty()) {
                removePackets.clear();

                for (DataPacket pk : packetEntry.addPackets) {
                    long id;
                    if (pk instanceof AddEntityPacket) {
                        id = ((AddEntityPacket) pk).entityRuntimeId;
                    } else if (pk instanceof SetEntityDataPacket) {
                        id = ((SetEntityDataPacket) pk).eid;
                    } else
                        continue;

                    RemoveEntityPacket rpk = new RemoveEntityPacket();
                    rpk.eid = id;

                    rpk.encode();
                    rpk.isEncoded = true;

                    removePackets.add(rpk);
                }

                Collections.reverse(removePackets);
                entityEntry.cacheRemovedPackets(removePackets);
            }
        }

        for (SinglePlayerEntry spe : packets) {
            List<RemoveEntityPacket> remove = new ArrayList<>();
            List<DataPacket> add = new ArrayList<>();

            for (EntityPacketEntry epe : spe.packetEntries) {
                remove.addAll(epe.removePackets);
                add.addAll(epe.addPackets);
            }

            List<DataPacket> sendPackets = new ArrayList<>(remove);
            sendPackets.addAll(add);
            spe.sendPackets = sendPackets.toArray(new DataPacket[0]);
        }

//        MainLogger.getLogger().info("packets: "+packets);
        for (SinglePlayerEntry entry : packets) {
            this.sendPackets(entry.sendPackets, entry.player);
        }
    }

    private SinglePlayerEntry spawnHologramTo(PlayerEntry playerEntry, UpdateEntry updateEntry, Collection<EntityEntry> entities) {
        SinglePlayerEntry packetEntry = new SinglePlayerEntry(playerEntry.player);

        List<List<String>> rawTranslations = updateEntry.translations;
        if (rawTranslations.isEmpty()) {
            return packetEntry;
        }

        int lang = playerEntry.language;
        if (lang >= rawTranslations.size()) {
            lang = 0;
        }

        List<String> old = rawTranslations.get(lang);
        List<String> lines = replaceTranslation(old, updateEntry.getPlayerPlaceholders().get(playerEntry.player.getId()));

        if (lines.isEmpty()) {
            return packetEntry;
        }

        List<List<String>> oldLines = updateEntry.getOldLines();

        if (oldLines != null && oldLines.size() > lang && lines.equals(oldLines.get(lang))) {
            return packetEntry;
        }

        int entityCount = 0;
        for (EntityEntry entityEntry : entities) {
            long[] reservedIds = updateEntry.entityIds[entityCount++];
            List<RemoveEntityPacket> reps = entityEntry.getRemovePackets();

            Vector3 pos = entityEntry.getSafePos();

            EntityPacketEntry entityPacketEntry = new EntityPacketEntry(entityEntry);

            List<DataPacket> packets = new ArrayList<>();
            if (!updateEntry.spawn && reps.size() > 0) {

                int i = 0;
                for (RemoveEntityPacket rep : reps) {
                    SetEntityDataPacket pk = new SetEntityDataPacket();
                    pk.eid = rep.eid;
                    pk.metadata = new EntityMetadata()
                            .putString(Entity.DATA_NAMETAG, lines.get(i++));

                    pk.encode();
                    pk.isEncoded = true;

                    packets.add(pk);
                }
            } else {
                packets = (List) compile(lines, pos, reservedIds, updateEntry.spawn);
            }

            entityEntry.clearCachedPackets();

            if (!reps.isEmpty() && updateEntry.spawn) {
                entityPacketEntry.removePackets.addAll(entityEntry.getRemovePackets());
            }

            entityPacketEntry.addPackets.addAll(packets);

            packetEntry.packetEntries.add(entityPacketEntry);
        }

        return packetEntry;
    }

    private List<List<String>> addPlaceHolders(List<List<String>> data, Map<String, String> placeHolders) {
        List<List<String>> trans = new ArrayList<>();

        for (List<String> translation : data) {
            trans.add(replaceTranslation(translation, placeHolders));
        }

        return trans;
    }

    private List<String> replaceTranslation(List<String> translation, Map<String, String> placeHolders) {
        List<String> replaced = new ArrayList<>();

        for (String origin : translation) {
            for (Entry<String, String> replaceEntry : placeHolders.entrySet()) {
                origin = origin.replaceAll("%" + replaceEntry.getKey() + "%", replaceEntry.getValue());
            }

            replaced.add(origin);
        }

        return replaced;
    }

    private List<AddEntityPacket> compile(List<String> lines, Vector3 pos, long[] entitiyIds, boolean spawn) {
        float baseY = (float) pos.y;

        List<AddEntityPacket> packets = new ArrayList<>();
        if (lines.isEmpty()) {
            return packets;
        }

        lines = Lists.reverse(new ArrayList<>(lines));

        int i = 0;
        for (String line : lines) {
            if (i >= entitiyIds.length) {
                plugin.getLogger().error("Invalid hologram line count");
                return packets;
            }

            long id = entitiyIds[i++];

            AddEntityPacket pk = new AddEntityPacket();
            pk.entityUniqueId = id;
            pk.entityRuntimeId = id;
            pk.type = 61; //
            pk.x = (float) pos.x;
            pk.y = baseY;
            pk.z = (float) pos.z;
            pk.speedX = 0;
            pk.speedY = 0;
            pk.speedZ = 0;
            pk.yaw = 0;
            pk.pitch = 0;
            pk.metadata = DEFAULT_DATA.clone().putString(Entity.DATA_NAMETAG, line);
            pk.attributes = DEFAULT_ATTRIBUTES;

            pk.encode();
            pk.isEncoded = true;

            packets.add(pk);

            baseY += HologramConfiguration.getLineGap();
        }

        return packets;
    }

    /**
     * Method to update ( (re) spawn) hologram to player(s)
     * This method is called from the main thread
     *
     * @param hologram
     * @param players
     */
    public void update(Hologram hologram, List<List<String>> oldLines, List<EntityEntry> entityEntries, boolean spawn, Player... players) {
        Set<Player> pls = new HashSet<>(Arrays.asList(players));
        if (pls.isEmpty()) {
            for (EntityEntry entity : hologram.getEntities()) {
                pls.addAll(entity.getEntity().getViewers().values());
            }
        }

        update(hologram, oldLines, entityEntries, spawn, pls);
    }

    public void update(Hologram hologram, List<List<String>> oldLines, List<EntityEntry> entityEntries, boolean spawn, Collection<Player> players) {
        if (players.isEmpty()) {
            return;
        }

        List<List<String>> translations = new ArrayList<>(hologram.getRawTranslations());
        if (translations.size() <= 0) {
            //TODO: despawn probably?
            return;
        }

        entityEntries = new ArrayList<>(entityEntries);
        for (EntityEntry entityEntry : entityEntries) {
            Entity e = entityEntry.getEntity();

            entityEntry.setSafePos(new Vector3(e.x, e.y, e.z));
        }

        int arraySize = translations.get(0).size();
        long[][] entityIds = new long[entityEntries.size()][arraySize];

        boolean needRecompile = oldLines != null && !oldLines.isEmpty() && oldLines.get(0).size() != translations.get(0).size();

        if (needRecompile) {
            spawn = true;
            for (int i = 0; i < entityIds.length; i++) {
                for (int j = 0; j < arraySize; j++) {
                    entityIds[i][j] = Entity.entityCount++;
                }
            }
        } else {
            for (int i = 0; i < entityIds.length; i++) {
                EntityEntry entry = entityEntries.get(i);
                List<RemoveEntityPacket> reps = entry.getRemovePackets();

                if (reps.isEmpty() || reps.size() != arraySize) {
                    for (int j = 0; j < arraySize; j++) {
                        entityIds[i][j] = Entity.entityCount++;
                    }
                } else {
                    for (int j = 0; j < reps.size(); j++) {
                        entityIds[i][j] = reps.get(j).eid;
                    }
                }
            }
        }

        List<PlayerEntry> playersData = new ArrayList<>();

        for (Player p : players) {
            playersData.add(new PlayerEntry(p, plugin.getPlaceholderAdapter().getLanguage(p)));
        }

        synchronized (updateQueue) {
            updateQueue.add(new UpdateEntry(hologram.getName(), hologram.isVisitorSensitive(), spawn, needRecompile, oldLines, translations, entityIds, entityEntries, playersData, hologram.updatePlaceholders(), hologram.updatePlayerPlaceholders(players)));
        }
    }

    public void updatePos(EntityEntry entityEntry, Vector3 pos) {
        synchronized (this.moveQueue) {
            this.moveQueue.add(new MoveEntry(entityEntry, pos.clone(), new ArrayList<>(entityEntry.getEntity().getViewers().values())));
        }
    }

    private void sendPackets(final DataPacket[] packets, final Player... players) {
        if (plugin.getConfiguration().isAsyncBatch()) {
            this.batchPackets(players, packets);
            return;
        }

        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            Server server = plugin.getServer();

            server.batchPackets(players, packets);
        });
    }

    public void batchPackets(final Player[] players, final DataPacket[] packets) {
        if (players == null || packets == null || players.length == 0 || packets.length == 0) {
            return;
        }

        try {
            byte[][] payload = new byte[packets.length * 2][];

            for (int i = 0; i < packets.length; i++) {
                DataPacket p = packets[i];
                if (!p.isEncoded) {
                    p.encode();
                }
                byte[] buf = p.getBuffer();
                payload[i * 2] = Binary.writeUnsignedVarInt(buf.length);
                payload[i * 2 + 1] = buf;
                packets[i] = null;
            }

            BatchPacket batch = new BatchPacket();
            batch.payload = Zlib.deflate(Binary.appendBytes(payload), 7);

            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                for (Player p : players) {
                    p.dataPacket(batch);
                }
            });
        } catch (Exception e) {
            MainLogger.getLogger().logException(e);
        }
    }

    @AllArgsConstructor
    @Getter
    @ToString
    private static class UpdateEntry {

        private final String hologramId;
        private final boolean visitorSensitive;
        private final boolean spawn;
        private final boolean recache;

        private final List<List<String>> oldLines;

        private final List<List<String>> translations;
        private final long[][] entityIds;
        private final Collection<EntityEntry> entityEntries;

        private final Collection<PlayerEntry> players;
        private Map<String, String> placeholders;
        private Map<Long, Map<String, String>> playerPlaceholders;

    }

    @AllArgsConstructor
    @ToString
    private static class PlayerEntry {

        private Player player;
        private int language;
    }

    @ToString
    private static class PacketsEntry {
        private final List<Player> players = new ArrayList<>();

        private final List<DataPacket> packets = new ArrayList<>();
    }

    @RequiredArgsConstructor
    @ToString
    private static class SinglePlayerEntry {

        private final Player player;

        private final List<EntityPacketEntry> packetEntries = new ArrayList<>();

        private DataPacket[] sendPackets;
    }

    @RequiredArgsConstructor
    @ToString
    private static class EntityPacketEntry {

        private final EntityEntry entityEntry;

        private final List<RemoveEntityPacket> removePackets = new ArrayList<>();
        private final List<DataPacket> addPackets = new ArrayList<>();
    }

    @RequiredArgsConstructor
    @ToString
    private static class MoveEntry {

        private final EntityEntry entityEntry;
        private final Vector3 pos;

        private final Collection<Player> players;
    }
}