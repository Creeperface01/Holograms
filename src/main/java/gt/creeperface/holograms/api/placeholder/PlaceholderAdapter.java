package gt.creeperface.holograms.api.placeholder;

import cn.nukkit.Player;
import gt.creeperface.holograms.placeholder.MatchedPlaceholder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author CreeperFace
 */
public interface PlaceholderAdapter<T extends MatchedPlaceholder> {

    Map<Long, Map<String, String>> translatePlaceholders(Collection<T> placeholders, Collection<Player> players);

    Map<String, String> translatePlaceholders(Collection<T> placeholders);

    boolean containsVisitorSensitivePlaceholder(Collection<T> placeholders);

    int getLanguage(Player p);

    boolean supports();

    Object getValue(String placeholder);

    List<T> matchPlaceholders(String text);

//    String replaceString(String input, List<MatchedPlaceholder> matched);
}
