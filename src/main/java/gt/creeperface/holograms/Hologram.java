package gt.creeperface.holograms;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.AddPlayerPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import gt.creeperface.holograms.entity.HologramEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author CreeperFace
 */
public class Hologram implements gt.creeperface.holograms.api.Hologram {

    private final List<EntityEntry> spawnedEntities = new ArrayList<>();

    private final List<HologramTranslation> translations = new ArrayList<>();
    //private final List<HologramPage> pages = new ArrayList<>();

    private final Map<String, Supplier<String>> placeHolders = new HashMap<>();
    private final Map<String, Function<Player, String>> playerPlaceHolders = new HashMap<>();

    @Getter
    private final List<List<AddPlayerPacket>> cachedPackets = new ArrayList<>();

    @Getter
    private final List<RemoveEntityPacket> cachedRemovePackets = new ArrayList<>();

    @Getter
    private final String name;

    @Getter
    private int updateInterval = -1;

    public Hologram(final String name, final List<List<String>> pages) {
        for (List<String> trans : pages) {
            this.translations.add(new HologramTranslation(trans));
        }

        this.name = name;
    }

    public List<List<String>> getRawTranslations() {
        return translations.stream()
                .map(HologramTranslation::getLines)
                .collect(Collectors.toList());
    }

    public List<EntityEntry> getEntities() {
        /*ArrayList<Vector3> list = new ArrayList<>();

        if(Server.getInstance().isPrimaryThread()) {
            for (Entity entity : spawnedEntities) {
                list.add(new Vector3(entity.x, entity.y, entity.z));
            }
        } else {
            synchronized (spawnedEntities) {
                for (Entity entity : spawnedEntities) {
                    list.add(new Vector3(entity.x, entity.y, entity.z));
                }
            }
        }*/
        synchronized (this.spawnedEntities) {
            return spawnedEntities;
        }
    }

    public Map<String, String> updatePlaceholders() {
        Map<String, String> result = new HashMap<>();

        placeHolders.forEach((k, v) -> result.put(k, v.get()));
        //Holograms.getInstance().placeholders.forEach((k, v) -> result.put(k, v.get()));

        return result;
    }

    public Map<Long, Map<String, String>> updatePlayerPlaceholders(Entity entity) {
        return updatePlayerPlaceholders(entity.getViewers().values());
    }

    public Map<Long, Map<String, String>> updatePlayerPlaceholders(final Collection<Player> players) {
        Map<Long, Map<String, String>> result = new HashMap<>();

        if (players != null && !players.isEmpty()) {
            playerPlaceHolders.forEach((k, v) -> players.forEach((p) -> {
                Map<String, String> m = result.get(p.getId());
                if (m == null) {
                    m = new HashMap<>();
                    result.put(p.getId(), m);
                }

                m.put(k, v.apply(p));
            }));
        }

        return result;
    }

    public void addEntity(HologramEntity entity) {
        EntityEntry entityEntry = new EntityEntry(entity);
        entity.setEntityEntry(entityEntry);

        synchronized (this.spawnedEntities) {
            this.spawnedEntities.add(entityEntry);
        }
    }

    public void removeEntity(HologramEntity entity) {
        synchronized (this.spawnedEntities) {
            this.spawnedEntities.remove(entity.getEntityEntry());
        }
    }

    public void spawnEntity(HologramEntity entity, Player... players) {
        Holograms.getInstance().hologramUpdater.update(entity.getHologram(), null, Arrays.asList(entity.getEntityEntry()), true, players);
    }

    public void despawnEntity(HologramEntity entity, Player... players) {
        EntityEntry entry = entity.getEntityEntry();

        synchronized (entry.removePackets) {
            if (!entry.removePackets.isEmpty()) { //shouldn't this be async too?
                Server.getInstance().batchPackets(players, entry.removePackets.stream().toArray(RemoveEntityPacket[]::new));
            }
        }
    }

    public void updatePos(HologramEntity entity) {
        EntityEntry entry = entity.getEntityEntry();
        Holograms.getInstance().hologramUpdater.updatePos(entry, new Vector3(entity.x, entity.y, entity.z));
    }

    public boolean isVisitorSensitive() {
        return !playerPlaceHolders.isEmpty();
    }

    public void update(List<List<String>> translations) {
        List<HologramTranslation> hologramTranslations = translations.stream()
                .map(HologramTranslation::new)
                .collect(Collectors.toList());

        this.translations.clear();
        this.translations.addAll(hologramTranslations);
    }

    @Override
    public gt.creeperface.holograms.api.HologramTranslation getTranslation(int index) {
        return this.translations.get(index);
    }

    @Override
    public Collection<gt.creeperface.holograms.api.HologramTranslation> getTranslations() {
        return new ArrayList<>(this.translations);
    }

    @Override
    public void setUpdateInterval(int updateInterval) {
        if (updateInterval == this.updateInterval)
            return;

        this.updateInterval = updateInterval;

        if (this.updateInterval > 0) {
            Holograms.getInstance().updateHolograms.put(getName(), this);
        } else {
            Holograms.getInstance().updateHolograms.remove(getName());
        }
    }

    public void reloadActivePlaceholders() {
        this.playerPlaceHolders.clear();
        this.placeHolders.clear();

        Holograms plugin = Holograms.getInstance();

        for (Entry<String, Supplier<String>> entry : plugin.placeholders.entrySet()) {
            String key = entry.getKey();

            transloop:
            for (HologramTranslation t : this.translations) {
                for (String l : t.getLines()) {
                    if (l.contains(key)) {
                        this.placeHolders.put(key, entry.getValue());
                        break transloop;
                    }
                }
            }
        }

        for (Entry<String, Function<Player, String>> entry : plugin.playerPlaceholders.entrySet()) {
            String key = entry.getKey();

            transloop:
            for (HologramTranslation t : this.translations) {
                for (String l : t.getLines()) {
                    if (l.contains(key)) {
                        this.playerPlaceHolders.put(key, entry.getValue());
                        break transloop;
                    }
                }
            }
        }
    }

    @Override
    public void update(Player... players) {
        Holograms.getInstance().hologramUpdater.update(this, getRawTranslations(), getEntities(), false, players);
    }

    public void updatePlaceHolder(String placeholder, Player... players) {
        if (players.length == 0) {

        }
    }

    @RequiredArgsConstructor
    public static class EntityEntry {

        @Getter
        private final Entity entity;
        @Getter
        @Setter
        private Vector3 safePos;

        private final List<List<AddPlayerPacket>> packets = new ArrayList<>();

        private final List<RemoveEntityPacket> removePackets = new ArrayList<>();

        public void cachePackets(List<List<AddPlayerPacket>> packets) {
            clearCachedPackets();
            this.packets.addAll(packets);
        }

        public void clearCachedPackets() {
            this.packets.clear();
        }

        public void cacheRemovedPackets(List<RemoveEntityPacket> packets) {
            synchronized (removePackets) {
                clearCachedRemovePackets();
                removePackets.addAll(packets);
            }
        }

        public void clearCachedRemovePackets() {
            synchronized (removePackets) {
                removePackets.clear();
            }
        }

        public List<List<AddPlayerPacket>> getPackets() {
            return new ArrayList<>(packets);
        }

        public List<RemoveEntityPacket> getRemovePackets() {
            return new ArrayList<>(removePackets);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EntityEntry) {
                EntityEntry another = (EntityEntry) obj;

                return entity.getId() == another.entity.getId();
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.entity.getId());
        }
    }
}
