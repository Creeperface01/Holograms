package gt.creeperface.holograms.placeholder;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import com.creeperface.nukkit.placeholderapi.PlaceholderAPIIml;
import com.creeperface.nukkit.placeholderapi.api.Placeholder;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import com.creeperface.nukkit.placeholderapi.api.event.PlaceholderAPIInitializeEvent;
import com.creeperface.nukkit.placeholderapi.api.scope.GlobalScope;
import com.creeperface.nukkit.placeholderapi.api.util.MatchedGroup;
import com.creeperface.nukkit.placeholderapi.api.util.UtilsKt;
import gt.creeperface.holograms.Holograms;
import gt.creeperface.holograms.api.Hologram;
import gt.creeperface.holograms.api.placeholder.PlaceholderAdapter;
import gt.creeperface.holograms.placeholder.PlaceholderAPIAdapter.MatchedPlaceholderLocal;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author CreeperFace
 */
public class PlaceholderAPIAdapter implements PlaceholderAdapter<MatchedPlaceholderLocal>, Listener {

    private final PlaceholderAPI api = PlaceholderAPIIml.getInstance();

    public PlaceholderAPIAdapter() {
        Holograms plugin = Holograms.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
//        Placeholder placeholder = api.getPlaceholder("lang");
//
//        if (placeholder != null) {
//            placeholder.addListener(plugin, (oldVal, newVal, p) -> plugin.onLanguageChanged(p));
//        }
    }

    @EventHandler
    public void onInit(PlaceholderAPIInitializeEvent e) {
        Holograms plugin = Holograms.getInstance();

        for (Hologram hologram : plugin.getHolograms().values()) {
            hologram.reloadActivePlaceholders();
        }
    }

    @Override
    public Map<Long, Map<String, String>> translatePlaceholders(Collection<MatchedPlaceholderLocal> placeholders, Collection<Player> players) {
        Map<Long, Map<String, String>> values = new HashMap<>();

        List<Entry> entries = new LinkedList<>();
        for (MatchedPlaceholderLocal match : placeholders) {
            Placeholder<?> placeholder = api.getPlaceholder(match.name);

            if (placeholder == null || !placeholder.isVisitorSensitive()) {
                continue;
            }

            entries.add(new Entry(match, placeholder));
        }

        for (Player player : players) {
            Map<String, String> replaced = new HashMap<>();

            for (Entry entry : entries) {
                replaced.put(
                        entry.match.raw,
                        entry.placeholder.getValue(entry.match.group.getParams(), GlobalScope.INSTANCE.getDefaultContext(), player));
            }

            values.put(player.getId(), replaced);
        }

        return values;
    }

    @Override
    public Map<String, String> translatePlaceholders(Collection<MatchedPlaceholderLocal> placeholders) {
        Map<String, String> values = new HashMap<>();

        for (MatchedPlaceholderLocal match : placeholders) {
            Placeholder<?> placeholder = api.getPlaceholder(match.name);

            if (placeholder == null || placeholder.isVisitorSensitive()) {
                continue;
            }

            values.put(match.raw, placeholder.getValue(match.group.getParams(), GlobalScope.INSTANCE.getDefaultContext(), null));
        }

        return values;
    }

    @Override
    public boolean containsVisitorSensitivePlaceholder(Collection<MatchedPlaceholderLocal> placeholders) {
        for (MatchedPlaceholderLocal pl : placeholders) {
            Placeholder<?> placeholder = api.getPlaceholder(pl.group.getValue());

            if (placeholder != null && placeholder.isVisitorSensitive()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<MatchedPlaceholderLocal> matchPlaceholders(String text) {
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
        Placeholder<?> placeholder = api.getPlaceholder("lang");

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
        Placeholder<?> p = api.getPlaceholder(placeholder);

        if (p != null) {
            return p.getDirectValue(null);
        }

        return null;
    }

    @Override
    public boolean supports() {
        return true;
    }

    @RequiredArgsConstructor
    private static class Entry {
        public final MatchedPlaceholderLocal match;
        public final Placeholder<?> placeholder;
    }

    @ToString(callSuper = true)
    public static class MatchedPlaceholderLocal extends MatchedPlaceholder {

        private final MatchedGroup group;

        public MatchedPlaceholderLocal(MatchedGroup matchedGroup) {
            super(matchedGroup.getRaw(), matchedGroup.getValue(), matchedGroup.getStart(), matchedGroup.getEnd());
            this.group = matchedGroup;
        }
    }
}
