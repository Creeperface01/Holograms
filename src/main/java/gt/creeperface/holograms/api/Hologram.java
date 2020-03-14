package gt.creeperface.holograms.api;

import cn.nukkit.Player;

import java.util.Collection;

/**
 * @author CreeperFace
 */
public interface Hologram {

    /**
     * Get {@link HologramTranslation} instance from its index
     *
     * @param index translation index
     * @return Translation instance
     */
    HologramTranslation getTranslation(int index);

    /**
     * @return collection of all translations
     */
    Collection<HologramTranslation> getTranslations();

    /**
     * Set how often should be hologram updated automatically
     *
     * @param updateInterval auto-update interval in ticks (set to -1 to disable auto updates)
     */
    void setUpdateInterval(int updateInterval);

    /**
     * Force update the hologram - send current state to all viewers
     */
    void update(Player... players);

    void reloadActivePlaceholders();

}
