package gt.creeperface.holograms;


import cn.nukkit.Player;
import cn.nukkit.Server;
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
import cn.nukkit.utils.MainLogger;
import gt.creeperface.holograms.api.HologramAPI;
import gt.creeperface.holograms.api.grid.source.GridSource;
import gt.creeperface.holograms.api.placeholder.PlaceholderAdapter;
import gt.creeperface.holograms.command.HologramCommand;
import gt.creeperface.holograms.entity.HologramEntity;
import gt.creeperface.holograms.form.FormWindowHandler;
import gt.creeperface.holograms.form.FormWindowManager;
import gt.creeperface.holograms.grid.CharactersTable;
import gt.creeperface.holograms.grid.source.AbstractGridSource;
import gt.creeperface.holograms.grid.source.ExampleGridSource;
import gt.creeperface.holograms.grid.source.MySQLGridSource;
import gt.creeperface.holograms.grid.source.PlaceholderGridSource;
import gt.creeperface.holograms.placeholder.DefaultPlaceholderAdapter;
import gt.creeperface.holograms.placeholder.MatchedPlaceholder;
import gt.creeperface.holograms.placeholder.PlaceholderAPIAdapter;
import gt.creeperface.holograms.task.HologramUpdater;
import gt.creeperface.holograms.util.Values;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author CreeperFace
 */
public class Holograms extends HologramAPI implements Listener {

    private static boolean loaded = false;

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
    private PlaceholderAdapter<MatchedPlaceholder> placeholderAdapter;

    private Map<String, BiFunction<AbstractGridSource.SourceParameters, Map<String, Object>, GridSource>> gridSources = new HashMap<>();
    private Map<String, Supplier<GridSource<Object>>> gridSourceInstances = new HashMap<>();

    @Setter
    private static Function<Player, Integer> languageHandler = null;

    @Override
    public void onLoad() {
        if (loaded) return;
    }

    @Override
    public void onEnable() {
        if (loaded) return;
        getLogger().info("Loading characters...");
        loadCharsWidths();

        Entity.registerEntity("Hologram", HologramEntity.class);
        instance = this;
        HologramAPI.instance = this;

        getLogger().info("Loading config...");
        checkConfig();
        this.configuration = new HologramConfiguration(this);

        saveResource("holograms.yml");
        path = new File(getDataFolder(), "holograms.yml");

        getLogger().info("Loading placeholders");
        initPlaceholderAdapter();

        //init grid sources
        getLogger().info("Registering default grid sources");
        registerDefaultGridSources();

        getLogger().info("Loading grid config...");
        registerGridSourceInstances();

        getServer().getCommandMap().register("hologram", new HologramCommand(this));
        getServer().getPluginManager().addPermission(new Permission("hologram.use", "Main holograms permission"));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleDelayedRepeatingTask(
                this,
                () -> saveHolograms(true),
                this.configuration.getSaveInterval() * 60 * 20,
                this.configuration.getSaveInterval() * 60 * 20
        );

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

        getLogger().info("Loading holograms");
        reloadHolograms();

        hologramUpdater.start();

        loaded = true;
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

                Hologram.GridSettings grid = hologram.getGridSettings();
                hl.set("grid", grid.isEnabled());
                hl.set("grid_normalize", grid.isNormalize());
                hl.set("grid_col_space", grid.getColumnSpace());
                hl.set("grid_source", grid.getSource() != null ? grid.getSource().getName() : "");
                hl.set("grid_header", grid.isHeader());

                hl.set("column_templates", grid.getColumnTemplates().stream()
                        .map((template) -> {
                            Map<String, Object> data = new HashMap<>();

                            if (template.name != null) {
                                data.put("name", template.name);
                            }

                            data.put("value", template.template);

                            return data;
                        }).collect(Collectors.toList())
                );

                holograms.set(hologram.getName(), hl);
            }
        }

        config.set("holograms", holograms);
        config.set("version", 2);

        config.save(async);
    }

    @SuppressWarnings("unchecked")
    public void reloadHolograms() {
        /*
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
                boolean grid = section.getBoolean("grid", false);
                int gridColSpace = section.getInt("grid_col_space", 20);
                String gridSource = section.getString("grid_source", "");
                boolean gridHeader = section.getBoolean("grid_header", false);
                boolean normalize = section.getBoolean("grid_normalize", false);

                checkLineCount(trans, id);

                List<Map<String, Object>> colTemplates = section.getList("column_templates", Collections.emptyList());

                List<Hologram.GridSettings.ColumnTemplate> templates = colTemplates.stream()
                        .map((data) -> {
                            String name = Objects.toString(data.get("name"));
                            String template = Objects.toString(data.get("value"));

                            int dataIndex = template.indexOf(Values.COLUMN_TEMPLATE_PLACEHOLDER);

                            return new Hologram.GridSettings.ColumnTemplate(name, template, dataIndex, dataIndex + Values.COLUMN_TEMPLATE_PLACEHOLDER.length());
                        }).collect(Collectors.toList());

                Hologram hologram = new Hologram(id, trans, new Hologram.GridSettings(grid, normalize, getGridSource(gridSource), gridColSpace, gridHeader, templates));
                hologram.setUpdateInterval(updateInterval);

                map.put(id, hologram);
            }
        }

        synchronized (hologramLock) {
            this.holograms = map;
        }
        */
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
                Hologram hologram = new Hologram(id, trans, new Hologram.GridSettings());

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

        this.handler.handleResponse(p, e.getFormID(), e.getWindow(), response);
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

    private void registerGridSourceInstances() {
        saveResource("grids.yml");
        Config grids = new Config(new File(getDataFolder(), "grids.yml"), Config.YAML);

        ConfigSection section = grids.getSection("grids");

        if (section == null) {
            return;
        }

        for (Entry<String, Object> entry : section.entrySet()) {
            String name = entry.getKey();
            ConfigSection value = (ConfigSection) entry.getValue();

            if (value == null || value.isEmpty()) {
                getLogger().warning("Grid " + name + " doesn't contain all required data");
                continue;
            }

            String source = value.getString("source");

            if (source == null || source.isEmpty()) {
                getLogger().warning("Grid " + name + " doesn't contain all required data");
                continue;
            }

            int offset = value.getInt("offset", 0);
            int limit = value.getInt("limit", 10);
            int refresh = value.getInt("refresh", 10);

            Map<String, Object> data = value.getSection("data");
            registerGridSource(new AbstractGridSource.SourceParameters(name, offset, limit, refresh), source, data);
        }
    }

    public boolean registerGridSourceClass(String identifier, Class<? extends GridSource> clazz) {
        if (gridSources.containsKey(identifier)) return false;

        BiFunction<AbstractGridSource.SourceParameters, Map<String, Object>, GridSource> factory = null;

        try {
            construct_loop:
            for (Constructor<?> cons : clazz.getDeclaredConstructors()) {
                cons.setAccessible(true);

                final List<Object> args = new ArrayList<>();
                int dataIndex = -1;
                int paramsIndex = -1;

                Class[] params = cons.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    Class param = params[i];

                    if (AbstractGridSource.SourceParameters.class.isAssignableFrom(param)) {
                        args.add(null);
                        paramsIndex = i;
                    } else if (Holograms.class.isAssignableFrom(param)) {
                        args.add(this);
                    } else if (Map.class.isAssignableFrom(param)) {
                        args.add(null);
                        dataIndex = i;
                    } else if (Server.class.isAssignableFrom(param)) {
                        args.add(getServer());
                    } else {
                        getLogger().warning("Class " + param.getName() + " cannot be passed as a source constructor argument");
                        continue construct_loop;
                    }
                }

                if (dataIndex != -1 || paramsIndex != -1) {
                    final int _dataIndex = dataIndex;
                    final int _paramsIndex = paramsIndex;

                    factory = (parameters, data) -> {
                        if (_dataIndex >= 0) {
                            args.set(_dataIndex, data);
                        }

                        if (_paramsIndex >= 0) {
                            args.set(_paramsIndex, parameters);
                        }

                        try {
                            return (GridSource) cons.newInstance(args.toArray());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };
                } else {
                    factory = (name, data) -> {
                        try {
                            return (GridSource) cons.newInstance(args.toArray());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };
                }

                break;
            }
        } catch (Exception e) {
            MainLogger.getLogger().logException(e);
            return false;
        }

        if (factory == null) {
            getLogger().warning("Constructor with valid arguments could not be found for source (" + identifier + ")");
            return false;
        }

        return gridSources.put(identifier, factory) != null;
    }

    public boolean registerGridSourceClass(String identifier, BiFunction<AbstractGridSource.SourceParameters, Map<String, Object>, GridSource> factory) {
        if (gridSources.containsKey(identifier)) return false;

        return gridSources.put(identifier, factory) != null;
    }

    private void registerDefaultGridSources() {
        registerGridSourceClass("example", ExampleGridSource.class);
        registerGridSourceClass("mysql", MySQLGridSource.class);

        if (this.placeholderAdapter.supports()) {
            registerGridSourceClass("placeholder", PlaceholderGridSource.class);
        }
    }

    public boolean registerGridSource(AbstractGridSource.SourceParameters parameters, String sourceIdentifier, Map<String, Object> data) {
        BiFunction<AbstractGridSource.SourceParameters, Map<String, Object>, GridSource> factory = this.gridSources.get(sourceIdentifier);

        if (factory == null) {
            getLogger().warning("Registering source instance(" + parameters.name + ") with unknown source(" + sourceIdentifier + ")");
            return false;
        }

        this.gridSourceInstances.put(parameters.name, () -> factory.apply(parameters, data));
        return true;
    }

    public GridSource<Object> getGridSource(String name) {
        Supplier<GridSource<Object>> source = this.gridSourceInstances.get(name);

        if (source != null) {
            return source.get();
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initPlaceholderAdapter() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderAdapter = (PlaceholderAdapter) new PlaceholderAPIAdapter();
            return;
        }

        this.placeholderAdapter = new DefaultPlaceholderAdapter();
    }

    private void checkConfig() {
        Config cfg = getConfig();

        int version = cfg.getInt("version");
        if (version > HologramConfiguration.VERSION) {
            getLogger().warning("Your hologram plugin isn't up to date or you changed the configuration version in config file (don't do this)");
            return;
        }

        if (version == HologramConfiguration.VERSION) {
            return;
        }

        Config latest = new Config(Config.YAML);
        latest.load(getResource("config.yml"));

        cfg.save();

        if (checkConfigSection(cfg.getRootSection(), latest.getRootSection())) {
            getLogger().info("Updating config.yml...");
            cfg.set("version", HologramConfiguration.VERSION);
            cfg.save();
        }
    }

    private boolean checkConfigSection(ConfigSection cfg, Map<String, Object> data) {
        boolean change = false;

        for (Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            Object cfgVal = cfg.get(key);

            if (!(cfgVal instanceof ConfigSection)) {
                cfg.set(key, val);
                change = true;
            } else if (val instanceof ConfigSection) {
                if (checkConfigSection((ConfigSection) cfgVal, ((ConfigSection) val).getAll())) {
                    change = true;
                }
            }
        }

        return change;
    }

    public int getLanguage(Player p) {
        if (languageHandler != null) {
            return languageHandler.apply(p);
        } else {
            return this.placeholderAdapter.getLanguage(p);
        }
    }

    private void loadCharsWidths() {
        try {
            CharactersTable.init(getResource("ascii.png"), getResource("glyph_sizes.bin"));
        } catch (IOException e) {
            MainLogger.getLogger().logException(e);
        }
    }
}
