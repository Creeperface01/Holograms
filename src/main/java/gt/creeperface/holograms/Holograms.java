package gt.creeperface.holograms;


import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.level.Level;
import cn.nukkit.permission.Permission;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import gt.creeperface.holograms.api.HologramAPI;
import gt.creeperface.holograms.command.HologramCommand;
import gt.creeperface.holograms.entity.HologramEntity;
import gt.creeperface.holograms.form.FormWindowHandler;
import gt.creeperface.holograms.form.FormWindowManager;
import gt.creeperface.holograms.placeholder.DefaultPlaceholderAdapter;
import gt.creeperface.holograms.placeholder.PlaceholderAPIAdapter;
import gt.creeperface.holograms.placeholder.PlaceholderAdapter;
import gt.creeperface.holograms.task.HologramUpdater;
import lombok.Getter;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author CreeperFace
 */
public class Holograms extends HologramAPI implements Listener {

    private final Object hologramLock = new Object();

    protected Map<String, Hologram> holograms = new HashMap<>();
    protected Map<String, Hologram> updateHolograms = new HashMap<>();

    public Map<Long, HologramEntity> editors = new HashMap<>();

    private File path;

    @Getter
    private static Holograms instance;

    @Getter
    private final FormWindowManager manager = new FormWindowManager(this);
    @Getter
    private final FormWindowHandler handler = new FormWindowHandler(this);

    protected final HologramUpdater hologramUpdater = new HologramUpdater(this);

    @Getter
    private HologramConfiguration configuration;

    @Getter
    private PlaceholderAdapter placeholderAdapter;

    @Override
    public void onLoad() {
        Entity.registerEntity("Hologram", HologramEntity.class);
        instance = this;
        HologramAPI.instance = this;

        saveDefaultConfig();
        this.configuration = new HologramConfiguration(this);

        saveResource("holograms.yml");
        path = new File(getDataFolder(), "holograms.yml");

        hologramUpdater.start();
    }

    @Override
    public void onEnable() {
        getServer().getCommandMap().register("hologram", new HologramCommand(this));
        getServer().getPluginManager().addPermission(new Permission("hologram.use", "Main holograms permission"));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleDelayedRepeatingTask(this, () -> {
            saveHolograms(true);
        }, this.configuration.getSaveInterval() * 60 * 20, this.configuration.getSaveInterval() * 60 * 20);

        getServer().getScheduler().scheduleRepeatingTask(this, new Task() {
            @Override
            public void onRun(int tick) {
                for (Hologram hologram : updateHolograms.values()) {
                    int interval = hologram.getUpdateInterval();

                    if (tick % interval == 0) {
                        hologramUpdater.update(hologram, hologram.getRawTranslations(), hologram.getEntities(), false);
                    }
                }
            }
        }, 1);

        initPlaceholderAdapter();
        reloadHolograms();


    }

    @Override
    public void onDisable() {
        saveHolograms(false);

        if (hologramUpdater.isAlive()) {
            hologramUpdater.interrupt();
        }
    }

    private void saveHolograms(boolean async) {
        Config config = new Config(path, Config.YAML);

        ConfigSection holograms = new ConfigSection();
        synchronized (hologramLock) {

            for (Hologram hologram : this.holograms.values()) {
                ConfigSection hl = new ConfigSection();
                hl.set("update", hologram.getUpdateInterval());
                hl.set("data", hologram.getRawTranslations());

                holograms.set(hologram.getName(), hl);
            }
        }

        config.set("holograms", holograms);
        config.set("version", 2);

        config.save(async);
    }

    @SuppressWarnings("unchecked")
    public void reloadHolograms() {
        Config config = new Config(path, Config.YAML);
        if (config.getInt("version", 0) < 2) {
            loadOldData(config);
            return;
        }

        Map<String, Hologram> map = new HashMap<>();
        ConfigSection holograms = config.getSection("holograms");

        if (holograms != null && !holograms.isEmpty()) {

            Map<String, ConfigSection> sections = (Map) holograms.getAllMap();
            for (Entry<String, ConfigSection> entry : sections.entrySet()) {
                String id = entry.getKey();
                ConfigSection section = entry.getValue();

                List<List<String>> trans = section.getList("data");
                for (List<String> lines : trans) {
                    for (int i = 0; i < lines.size(); i++) {
                        lines.set(i, lines.get(i).replaceAll("&", "§"));
                    }
                }

                int updateInterval = section.getInt("update", -1);

                checkLineCount(trans, id);

                Hologram hologram = new Hologram(id, trans);
                hologram.setUpdateInterval(updateInterval);

                map.put(id, hologram);
            }
        }

        synchronized (hologramLock) {
            this.holograms = map;
        }
    }

    private void checkLineCount(List<List<String>> trans, String hologramId) {
        if (trans.size() <= 0) {
            return;
        }

        int min = Integer.MAX_VALUE;
        int max = 0;

        for (List<String> t : trans) {
            int size = t.size();

            if (size < min)
                min = size;

            if (size > max)
                max = size;
        }

        if (min != max) {
            getLogger().warning("Detected different line count in hologram '" + hologramId + "', fixing...");

            for (List<String> t : trans) {
                int diff = max - t.size();

                while (diff > 0) {
                    diff--;

                    t.add("");
                }
            }
        }
    }

    private void loadOldData(Config config) {
        Map<String, Hologram> map = new HashMap<>();
        ConfigSection holograms = config.getSection("holograms");

        if (holograms != null && !holograms.isEmpty()) {

            Map<String, List> sections = (Map) holograms.getAllMap();
            for (Entry<String, List> entry : sections.entrySet()) {
                String id = entry.getKey();

                List<List<String>> trans = entry.getValue();
                for (List<String> lines : trans) {
                    for (int i = 0; i < lines.size(); i++) {
                        lines.set(i, lines.get(i).replaceAll("&", "§"));
                    }
                }

                checkLineCount(trans, id);
                Hologram hologram = new Hologram(id, trans);

                map.put(id, hologram);
            }
        }

        synchronized (hologramLock) {
            this.holograms = map;
        }

        saveHolograms(false);
    }

    @EventHandler
    public void onFormResponse(PlayerFormRespondedEvent e) {
        Player p = e.getPlayer();
        FormResponse response = e.getResponse();

        this.handler.handleResponse(p, e.getFormID(), response);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        editors.remove(p.getId());
    }

    /**
     * Should be called by language plugin when player changes their language
     * to update holograms
     *
     * @param p player which has changed their language
     */
    public void onLanguageChanged(Player p) {
        for (Entity entity : p.getLevel().getEntities()) {
            if (entity instanceof HologramEntity && entity.getViewers().containsKey(p.getLoaderId())) {
                HologramEntity hologramEntity = (HologramEntity) entity;

                this.hologramUpdater.update(hologramEntity.getHologram(), hologramEntity.getHologram().getRawTranslations(), Collections.singletonList(hologramEntity.getEntityEntry()), false, p);
            }
        }
    }

    public void update(String id, List<List<String>> lines) {
        synchronized (hologramLock) {
            Hologram hologram = holograms.get(id);

            boolean spawn = true;

            List<List<String>> old = hologram.getRawTranslations();
            if (old.size() > 0 && lines.size() > 0 && old.get(0).size() == lines.get(0).size()) {
                spawn = false;
            }

            hologram.update(lines);
            hologram.reloadActivePlaceholders();

            this.hologramUpdater.update(hologram, old, getEntitiesByHologram(id).stream().map(HologramEntity::getEntityEntry).collect(Collectors.toList()), spawn);
        }
    }

    public HologramEntity findNearEntity(Player center) {
        double distance = Long.MAX_VALUE;
        Entity near = null;

        for (Entity find : center.getLevel().getEntities()) {
            if (find instanceof HologramEntity) {
                double dist = center.distanceSquared(find);

                if (near == null || dist < distance) {
                    distance = dist;
                    near = find;
                }
            }
        }

        return (HologramEntity) near;
    }

    public Hologram getHologram(String id) {
        return holograms.get(id);
    }

    private Collection<HologramEntity> getEntitiesByHologram(String hologram) {
        List<HologramEntity> entities = new ArrayList<>();

        for (Level level : getServer().getLevels().values()) {
            for (Entity entity : level.getEntities()) {
                if (entity instanceof HologramEntity) {

                    HologramEntity hologramEntity = (HologramEntity) entity;
                    if (hologramEntity.getHologramId().equals(hologram)) {
                        entities.add(hologramEntity);
                    }
                }
            }
        }

        return entities;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, gt.creeperface.holograms.api.Hologram> getHolograms() {
        return new HashMap<>((Map) holograms);
    }

    public Map<String, Hologram> getInternalHolograms() {
        return this.holograms;
    }

    private void initPlaceholderAdapter() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderAdapter = new PlaceholderAPIAdapter();
            return;
        }

        this.placeholderAdapter = new DefaultPlaceholderAdapter();
    }
}
