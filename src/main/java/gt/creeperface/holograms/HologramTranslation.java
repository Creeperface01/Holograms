package gt.creeperface.holograms;

import com.creeperface.nukkit.placeholderapi.api.util.MatchedGroup;
import gt.creeperface.holograms.api.placeholder.PlaceholderAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * @author CreeperFace
 */

@RequiredArgsConstructor
public class HologramTranslation implements gt.creeperface.holograms.api.HologramTranslation {

    private final List<String> lines;

    @Getter
    private final List<List<PlaceholderAdapter.MatchedPlaceholder>> placeholders = new ArrayList<>();

    @Getter
    private final Set<String> placeholderNames = new HashSet<>();

    public String getLine(int index) {
        return lines.get(index);
    }

    public int getLineCount() {
        return lines.size();
    }

    public void addLine(int count) {
        for (; count > 0; count--) {
            lines.add("");
        }
    }

    public void addLines(Collection<String> l) {
        lines.addAll(l);
    }

    public void setLines(Collection<String> l) {
        lines.clear();
        lines.addAll(l);
    }

    public void removeLine(int count) {
        for (; count > 0; count--) {
            lines.remove(lines.size() - 1);
        }
    }

    public List<String> getLines() {
        return new ArrayList<>(lines);
    }

    public void mapPlaceholders() {
        this.placeholders.clear();
        this.placeholderNames.clear();

        for (String line : this.lines) {
            List<PlaceholderAdapter.MatchedPlaceholder> placeholders = Holograms.getInstance().getPlaceholderAdapter().matchPlaceholders(line);

            for (PlaceholderAdapter.MatchedPlaceholder p : placeholders) {
                this.placeholderNames.add(p.name);
            }

            this.placeholders.add(placeholders);
        }
    }

    @AllArgsConstructor
    public class LineEntry {

        private final String line;
        private final List<MatchedGroup> placeholders;
        private final List<String> values;

    }
}
