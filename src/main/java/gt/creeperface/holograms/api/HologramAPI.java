package gt.creeperface.holograms.api;

import cn.nukkit.Player;
import cn.nukkit.plugin.PluginBase;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author CreeperFace
 */
public abstract class HologramAPI extends PluginBase {

    protected static HologramAPI instance;

    /**
     * Add placeholder that can be used in holograms
     *
     * @param placeholder String which should be replaced in holograms
     * @param replacement Method that returns current placeholder value
     */
    public abstract void addPlaceHolder(final String placeholder, final Supplier<String> replacement);

    /**
     * Add visitor sensitive placeholder that can be used in holograms
     *
     * @param placeholder String which should be replaced in holograms
     * @param replacement Method that returns current placeholder value for certain {@link Player} instance
     */
    public abstract void addPlaceHolder(final String placeholder, final Function<Player, String> replacement);

    /**
     * Remove certain placeholder from available placeholders
     * All its occurrences in hologram won't be replaced and will be visible as placeholder key
     *
     * @param placeHolder
     */
    public abstract void removePlaceHolder(final String placeHolder);

    /**
     * This method have to be called if you made some changes to placeholders
     * If you don't call it, changes won't be visible
     */
    public abstract void reloadPlaceholders();

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
     * Should be called by language plugin when player changes their language
     * to update holograms
     *
     * @param p player which has changed their language
     */
    public abstract void onLanguageChanged(Player p);

    /**
     * This method is for multi-language providers to let Holograms know
     * what language player uses
     *
     * @param languageSelector {@link Function} returning language index from {@link Player} instance
     */
    public abstract void setLanguageSelector(Function<Player, Integer> languageSelector);

    /**
     * @return {@link HologramAPI} instance
     */
    public static HologramAPI getInstance() {
        return instance;
    }
}
