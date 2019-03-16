package gt.creeperface.holograms.placeholder;

import cn.nukkit.Player;
import gt.creeperface.holograms.api.placeholder.PlaceholderAdapter;

import java.util.*;

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
    public List<MatchedPlaceholder> matchPlaceholders(String text) {
        return Collections.emptyList();
    }

    @Override
    public int getLanguage(Player p) {
        return 0;
    }

    @Override
    public boolean supports() {
        return false;
    }

    @Override
    public Object getValue(String placeholder) {
        return null;
    }
}
