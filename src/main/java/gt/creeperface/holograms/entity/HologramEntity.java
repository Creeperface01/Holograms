package gt.creeperface.holograms.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import gt.creeperface.holograms.Hologram;
import gt.creeperface.holograms.Hologram.EntityEntry;
import gt.creeperface.holograms.Holograms;
import lombok.Getter;
import lombok.Setter;

/**
 * @author CreeperFace
 */
public class HologramEntity extends Entity {

    //public static long lastUpdate = 0;
    //private long updated = 0;

    @Getter
    private String hologramId;

    @Getter
    private Hologram hologram;

    @Getter
    @Setter
    private EntityEntry entityEntry;

    public HologramEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.keepMovement = true;
        this.hologramId = this.namedTag.getString("hologramId");
        this.hologram = Holograms.getInstance().getHologram(this.hologramId);

        if (this.hologram == null) {
            closeHologram();
            return;
        }

        this.hologram.addEntity(this);
    }

    public HologramEntity(FullChunk chunk, CompoundTag nbt, Hologram hologram) {
        super(chunk, nbt);

        this.keepMovement = true;
        this.hologramId = this.namedTag.getString("hologramId");
        this.hologram = hologram;

        if (this.hologram == null) {
            closeHologram();
            return;
        }

        this.hologram.addEntity(this);
    }

    @Override
    public int getNetworkId() {
        return -1;
    }

    /*public void reload() {
        List<List<String>> trans = getText(getHologramId());

        List<AddPlayerPacket[]> cachedPackets = new ArrayList<>();

        if (trans.isEmpty()) {
            return;
        }

        for (List<String> lines : trans) {
            float baseY = (float) this.getY();

            List<AddPlayerPacket> packets = new ArrayList<>();
            if (lines.isEmpty()) {
                continue;
            }

            lines = Lists.reverse(new ArrayList<>(lines));

            for (String line : lines) {
                long flags = (
                        (1L << Entity.DATA_FLAG_CAN_SHOW_NAMETAG) |
                                (1L << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG) |
                                (1L << Entity.DATA_FLAG_IMMOBILE)
                );
                EntityData data = (EntityData) new EntityData()
                        .putLong(Entity.DATA_FLAGS, flags)
                        .putFloat(Entity.DATA_BOUNDING_BOX_HEIGHT, 0)
                        .putFloat(Entity.DATA_BOUNDING_BOX_WIDTH, 0)
                        .putFloat(Entity.DATA_SCALE, 0.01f)
                        .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                        .putString(Entity.DATA_NAMETAG, line);
                data.putString(Entity.DATA_NAMETAG, line);

                long id = Entity.entityCount++;

                AddPlayerPacket pk = new AddPlayerPacket();
                pk.entityUniqueId = id;
                pk.entityRuntimeId = id;
                pk.item = Values.AIR;
                pk.x = (float) this.x;
                pk.y = baseY;
                pk.z = (float) this.z;
                pk.speedX = 0;
                pk.speedY = 0;
                pk.speedZ = 0;
                pk.yaw = 0;
                pk.username = line;
                pk.pitch = 0;
                pk.metadata = data;
                pk.uuid = UUID.randomUUID();

                pk.encode();
                pk.isEncoded = true;

                packets.add(pk);

                baseY += Values.LINE_SPACE;
            }

            cachedPackets.add(packets.stream().toArray(AddPlayerPacket[]::new));
        }

        this.despawnFromAll();

        if (cachedPackets.size() > 1) { //convert IDs
            AddPlayerPacket[] first = cachedPackets.get(0);

            for (int i = 1; i < cachedPackets.size(); i++) {
                AddPlayerPacket[] replaces = cachedPackets.get(i);

                for (int j = 0; j < first.length; j++) {
                    AddPlayerPacket replace = replaces[j];
                    replace.entityRuntimeId = first[j].entityRuntimeId;
                    replace.entityUniqueId = first[j].entityUniqueId;
                }
            }
        }

        this.cachedPackets = cachedPackets;
        this.spawnToAll();
    }*/

    @Override
    public boolean onUpdate(int currentTick) {
        if (closed) {
            return false;
        }

        /*if (lastUpdate > updated) {
            reload();
            updated = System.currentTimeMillis();
        }*/

        if (hologram == null) {
            closeHologram();
            return false;
        }

        return true;
    }

    /*public void respawnTo(Player p) {
        this.despawnFrom(p);

        spawnTo(p);
    }*/

    @Override
    public void spawnToAll() {
        if (this.chunk != null && !this.closed) {
            Player[] players = this.level.getChunkPlayers(this.chunk.getX(), this.chunk.getZ()).values().toArray(new Player[0]);

            this.hologram.spawnEntity(this, players);

            for (Player p : players) {
                this.hasSpawned.put(p.getLoaderId(), p);
            }
        }
    }

    @Override
    public void spawnTo(Player player) {
        if (!this.hasSpawned.containsKey(player.getLoaderId()) && player.usedChunks.containsKey(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            this.hasSpawned.put(player.getLoaderId(), player);

            this.hologram.spawnEntity(this, player);
            /*if (cachedPackets.size() > 0) {
                for (AddPlayerPacket pk : this.cachedPackets.get(cachedPackets.size() < 2 ? 0 : Holograms.getLanguage(player))) {
                    player.dataPacket(pk);
                }
            }*/
        }
    }

   /* public static List<List<String>> getText(String id) {
        synchronized (Holograms.hologramLock) {
            return new ArrayList<>(Holograms.holograms.getOrDefault(id, new ArrayList<>()));
        }
    }*/

    @Override
    public void despawnFromAll() {
        if (this.hologram != null) {
            Player[] players = this.hasSpawned.values().toArray(new Player[0]);

            this.hologram.despawnEntity(this, players);
        }

        this.hasSpawned.clear();
    }

    @Override
    public void despawnFrom(Player player) {
        if (this.hasSpawned.containsKey(player.getLoaderId())) {
            if (this.hologram != null) {
                this.hologram.despawnEntity(this, player);
            }

            this.hasSpawned.remove(player.getLoaderId());
        }
    }

    public void moveTo(double x, double y, double z) {
        this.setPosition(temporalVector.setComponents(x, y, z));

        this.hologram.updatePos(this);
    }

    public void offsetTo(double x, double y, double z) {
        this.move(x, y, z);

        this.hologram.updatePos(this);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        Exception e = new Exception();
        StackTraceElement element = e.getStackTrace()[1];
        String path = element.getClassName() + "." + element.getMethodName();

        if (!path.equals("cn.nukkit.level.format.generic.BaseFullChunk.unload")) {
            throw new UnsupportedOperationException("Cannot not close hologram entity. Use closeHologram() to perform entity deletion");
        }

        closeHologram();
    }

    public void closeHologram() {
        super.close();

        if (this.hologram != null)
            this.hologram.removeEntity(this);

        this.hasSpawned.clear();
    }
}