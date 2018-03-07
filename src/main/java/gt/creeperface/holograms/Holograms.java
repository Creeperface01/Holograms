package gt.creeperface.holograms;


import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.level.Level;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.permission.Permission;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.google.common.base.Preconditions;
import gt.creeperface.holograms.api.HologramAPI;
import gt.creeperface.holograms.command.HologramCommand;
import gt.creeperface.holograms.entity.HologramEntity;
import gt.creeperface.holograms.form.FormWindowHandler;
import gt.creeperface.holograms.form.FormWindowManager;
import gt.creeperface.holograms.task.HologramUpdater;
import lombok.Getter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
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

    private Function<Player, Integer> languageSelector;

    protected final HologramUpdater hologramUpdater = new HologramUpdater(this);

    protected final Map<String, Supplier<String>> placeholders = new HashMap<>();
    protected final Map<String, Function<Player, String>> playerPlaceholders = new HashMap<>();

    @Getter
    private HologramConfiguration configuration;

    @Override
    public void onLoad() {
        Entity.registerEntity("Hologram", HologramEntity.class);
        instance = this;
        HologramAPI.instance = this;

        languageSelector = (p) -> 0;

        initDefaultPlaceholders();

        saveDefaultConfig();
        this.configuration = new HologramConfiguration(this);

        saveResource("holograms.yml");
        path = new File(getDataFolder(), "holograms.yml");

        reloadHolograms();

        hologramUpdater.start();
    }

    @Override
    public void onEnable() {
        getServer().getCommandMap().register("hologram", new HologramCommand(this));
        getServer().getPluginManager().addPermission(new Permission("hologram.use", "Main holograms permission"));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleDelayedRepeatingTask(this, () -> {
            saveHolograms(true);
        }, this.configuration.getSaveInterval(), this.configuration.getSaveInterval());

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

        reloadPlaceholders();
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

        reloadPlaceholders();
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

        reloadPlaceholders();

        saveHolograms(false);
    }

    public void addPlaceHolder(final String string, final Supplier<String> replacement) {
        this.placeholders.put(string, replacement);
    }

    public void addPlaceHolder(final String string, final Function<Player, String> replacement) {
        this.playerPlaceholders.put(string, replacement);
    }

    public void removePlaceHolder(final String placeHolder) {
        placeholders.remove(placeHolder);
        playerPlaceholders.remove(placeHolder);
    }

    /**
     * should be called when any placeholder is added, removed or replaced with another one by plugin
     */
    public void reloadPlaceholders() {
        this.holograms.values().forEach(Hologram::reloadActivePlaceholders);
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

                this.hologramUpdater.update(hologramEntity.getHologram(), hologramEntity.getHologram().getRawTranslations(), Arrays.asList(hologramEntity.getEntityEntry()), false, p);
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
                double dist = Long.MAX_VALUE;

                if (near == null || (dist = center.distance(find)) < distance) {
                    distance = dist;
                    near = find;
                }
            }
        }

        return (HologramEntity) near;
    }

    public void setLanguageSelector(Function<Player, Integer> languageSelector) {
        Preconditions.checkNotNull(languageSelector, "Language selector cannot be set to null");
        this.languageSelector = languageSelector;
    }

    public int getLanguage(Player p) {
        return languageSelector.apply(p);
    }

    public Hologram getHologram(String id) {
        return holograms.get(id);
    }

    public Collection<HologramEntity> getEntitiesByHologram(String hologram) {
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

    private void initDefaultPlaceholders() {
        placeholders.put("%players", () -> Integer.toString(getServer().getOnlinePlayers().size()));
        placeholders.put("%tps%", () -> Double.toString(NukkitMath.round(getServer().getTicksPerSecond(), 1)));
        placeholders.put("%memory%", () -> {
            Runtime runtime = Runtime.getRuntime();

            return "" + ((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        });
        placeholders.put("%maxmemory%", () -> "" + (Runtime.getRuntime().totalMemory() / 1024 / 1024));

        this.placeholders.put("%time%", () ->
                new SimpleDateFormat("HH:mm:ss").format(new Date())
        );

        //player related placeholders
        playerPlaceholders.put("%player%", Player::getName);
        playerPlaceholders.put("%dplayer%", Player::getDisplayName);
        playerPlaceholders.put("%ping%", (p) -> p.isOnline() ? "" + p.getPing() : "-1");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, gt.creeperface.holograms.api.Hologram> getHolograms() {
        return new HashMap<>((Map) holograms);
    }

    public Map<String, Hologram> getInternalHolograms() {
        return this.holograms;
    }
}
