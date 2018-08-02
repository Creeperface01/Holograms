package gt.creeperface.holograms;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import com.creeperface.nukkit.placeholderapi.api.util.MatchedGroup;
import gt.creeperface.holograms.entity.HologramEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author CreeperFace
 */
public class Hologram implements gt.creeperface.holograms.api.Hologram {

    private final List<EntityEntry> spawnedEntities = new ArrayList<>();

    private final List<HologramTranslation> translations = new ArrayList<>();
    //private final List<HologramPage> pages = new ArrayList<>();

    private final List<MatchedGroup> placeholderMap = new ArrayList<>();

    private final Set<String> placeholders = new HashSet<>();

    @Getter
    private boolean visitorSensitive;

    @Getter
    private final String name;

    @Getter
    private int updateInterval = -1;

    public Hologram(final String name, final List<List<String>> pages) {
        this.name = name;

        for (List<String> trans : pages) {
            HologramTranslation translation = new HologramTranslation(trans);

            this.translations.add(translation);
        }

        this.reloadActivePlaceholders();
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
        return Holograms.getInstance().getPlaceholderAdapter().translatePlaceholders(this.placeholders);
    }

    public Map<Long, Map<String, String>> updatePlayerPlaceholders(Entity entity) {
        return updatePlayerPlaceholders(entity.getViewers().values());
    }

    public Map<Long, Map<String, String>> updatePlayerPlaceholders(final Collection<Player> players) {
        if (!isVisitorSensitive()) {
            return new HashMap<>();
        }

        return Holograms.getInstance().getPlaceholderAdapter().translatePlaceholders(this.placeholders, players);
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
        Holograms.getInstance().hologramUpdater.update(entity.getHologram(), null, Collections.singletonList(entity.getEntityEntry()), true, players);
    }

    public void despawnEntity(HologramEntity entity, Player... players) {
        EntityEntry entry = entity.getEntityEntry();

        synchronized (entry.removePackets) {
            if (!entry.removePackets.isEmpty()) { //shouldn't this be async too?
                Server.getInstance().batchPackets(players, entry.removePackets.toArray(new RemoveEntityPacket[0]));
            }
        }
    }

    public void updatePos(HologramEntity entity) {
        EntityEntry entry = entity.getEntityEntry();
        Holograms.getInstance().hologramUpdater.updatePos(entry, new Vector3(entity.x, entity.y, entity.z));
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
        this.placeholders.clear();

        this.translations.forEach(tr -> {
            tr.mapPlaceholders();
            this.placeholders.addAll(tr.getPlaceholders());
        });

        this.visitorSensitive = Holograms.getInstance().getPlaceholderAdapter().containsVisitorSensitivePlaceholder(this.placeholders);
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

        private final List<List<AddEntityPacket>> packets = new ArrayList<>();

        private final List<RemoveEntityPacket> removePackets = new ArrayList<>();

        public void cachePackets(List<List<AddEntityPacket>> packets) {
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

        public List<List<AddEntityPacket>> getPackets() {
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
