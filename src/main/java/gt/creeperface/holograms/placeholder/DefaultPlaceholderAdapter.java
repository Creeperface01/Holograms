package gt.creeperface.holograms.placeholder;

import cn.nukkit.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author CreeperFace
 */
public class DefaultPlaceholderAdapter implements PlaceholderAdapter {

    @Override
    public Map<String, String> translatePlaceholders(Collection<String> placeholders) {
        return new HashMap<>();
    }

    /**
     * shouldn't be called
     */
    @Override
    public Map<Long, Map<String, String>> translatePlaceholders(Collection<String> placeholders, Collection<Player> players) {
        return new HashMap<>();
    }

    @Override
    public boolean containsVisitorSensitivePlaceholder(Collection<String> placeholders) {
        return false;
    }

    @Override
    public int getLanguage(Player p) {
        return 0;
    }
}
