package gt.creeperface.holograms.placeholder;

import cn.nukkit.Player;

import java.util.Collection;
import java.util.Map;

/**
 * @author CreeperFace
 */
public interface PlaceholderAdapter {

    Map<Long, Map<String, String>> translatePlaceholders(Collection<String> placeholders, Collection<Player> players);

    Map<String, String> translatePlaceholders(Collection<String> placeholders);

    boolean containsVisitorSensitivePlaceholder(Collection<String> placeholders);

    int getLanguage(Player p);
}
