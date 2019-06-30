package gt.creeperface.holograms.placeholder;

import cn.nukkit.Player;
import com.creeperface.nukkit.placeholderapi.PlaceholderAPIIml;
import com.creeperface.nukkit.placeholderapi.api.Placeholder;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import com.creeperface.nukkit.placeholderapi.api.util.MatchedGroup;
import com.creeperface.nukkit.placeholderapi.api.util.UtilsKt;
import gt.creeperface.holograms.Holograms;
import gt.creeperface.holograms.api.placeholder.PlaceholderAdapter;
import lombok.ToString;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author CreeperFace
 */
public class PlaceholderAPIAdapter implements PlaceholderAdapter {

    private final PlaceholderAPI api = PlaceholderAPIIml.getInstance();

    @SuppressWarnings("unchecked")
    public PlaceholderAPIAdapter() {
        Holograms plugin = Holograms.getInstance();
//        Placeholder placeholder = api.getPlaceholder("lang");
//
//        if (placeholder != null) {
//            placeholder.addListener(plugin, (oldVal, newVal, p) -> plugin.onLanguageChanged(p));
//        }
    }

    @Override
    public Map<Long, Map<String, String>> translatePlaceholders(Collection<String> placeholders, Collection<Player> players) {
        List<Placeholder> instances = api.getPlaceholders().entrySet().stream().filter(entry -> entry.getValue().isVisitorSensitive() && placeholders.contains(entry.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());

        Map<Long, Map<String, String>> translations = new HashMap<>();

        players.forEach(p -> {
            Map<String, String> replaced = new HashMap<>();

            instances.forEach(hologram -> replaced.put(hologram.getName(), hologram.getValue(p)));

            translations.put(p.getId(), replaced);
        });

        return translations;
    }

    @Override
    public Map<String, String> translatePlaceholders(Collection<String> placeholders) {
        List<Placeholder> instances = api.getPlaceholders().entrySet().stream().filter(entry -> !entry.getValue().isVisitorSensitive() && placeholders.contains(entry.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());

        Map<String, String> translations = new HashMap<>();

        for (Placeholder placeholder : instances) {
            translations.put(placeholder.getName(), placeholder.getValue());
        }

        return translations;
    }

    @Override
    public boolean containsVisitorSensitivePlaceholder(Collection<String> placeholders) {
        for (String pl : placeholders) {
            Placeholder placeholder = api.getPlaceholder(pl);

            if (placeholder != null && placeholder.isVisitorSensitive()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<MatchedPlaceholder> matchPlaceholders(String text) {
        return UtilsKt.matchPlaceholders(text).stream().map(MatchedPlaceholderLocal::new).collect(Collectors.toList());
    }

//    @Override
//    @SuppressWarnings("unchecked")
//    public String replaceString(String input, List<MatchedPlaceholder> matched) {
//        List<MatchedPlaceholderLocal> local = (List) matched;
//
//        return null;
//    }

    @Override
    public int getLanguage(Player p) {
        Placeholder placeholder = api.getPlaceholder("lang");

        if (placeholder != null) {
            String o = placeholder.getValue(p);
            int lang;

            try {
                lang = Integer.parseInt(o);
                return lang;
            } catch (NumberFormatException e) {
                //probably wrong lang placeholder?
            }
        }

        return 0;
    }

    @Override
    public Object getValue(String placeholder) {
        Placeholder p = api.getPlaceholder(placeholder);

        if (p != null) {
            return p.getDirectValue(null);
        }

        return null;
    }

    @Override
    public boolean supports() {
        return true;
    }

    @ToString(callSuper = true)
    public static class MatchedPlaceholderLocal extends MatchedPlaceholder {

        private final MatchedGroup group;

        public MatchedPlaceholderLocal(MatchedGroup matchedGroup) {
            super(matchedGroup.getValue(), matchedGroup.getStart(), matchedGroup.getEnd());
            this.group = matchedGroup;
        }
    }
}
