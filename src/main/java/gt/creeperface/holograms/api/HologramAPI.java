package gt.creeperface.holograms.api;

import cn.nukkit.plugin.PluginBase;

import java.util.Map;

/**
 * @author CreeperFace
 */
public abstract class HologramAPI extends PluginBase {

    protected static HologramAPI instance;

    /**
     * Get hologram with specified ID
     *
     * @param id hologram ID
     * @return {@link Hologram} instance of this ID
     */
    public abstract Hologram getHologram(String id);

    /**
     * Get all registered holograms
     *
     * @return Map of keys and Hologram instances
     */
    public abstract Map<String, Hologram> getHolograms();

    /**
     * @return {@link HologramAPI} instance
     */
    public static HologramAPI getInstance() {
        return instance;
    }
}
