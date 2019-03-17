package gt.creeperface.holograms.api.placeholder;

import cn.nukkit.Player;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author CreeperFace
 */
public interface PlaceholderAdapter {

    Map<Long, Map<String, String>> translatePlaceholders(Collection<String> placeholders, Collection<Player> players);

    Map<String, String> translatePlaceholders(Collection<String> placeholders);

    boolean containsVisitorSensitivePlaceholder(Collection<String> placeholders);

    int getLanguage(Player p);

    boolean supports();

    Object getValue(String placeholder);

    List<MatchedPlaceholder> matchPlaceholders(String text);

//    String replaceString(String input, List<MatchedPlaceholder> matched);

    @RequiredArgsConstructor
    @ToString
    abstract class MatchedPlaceholder implements Cloneable {
        public final String name;
        public final int start;
        public final int end;

        public int offset = 0;

        @Override
        public MatchedPlaceholder clone() {
            try {
                return (MatchedPlaceholder) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }
}
