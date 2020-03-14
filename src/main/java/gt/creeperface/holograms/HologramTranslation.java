package gt.creeperface.holograms;

import gt.creeperface.holograms.placeholder.MatchedPlaceholder;
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
    private final List<List<MatchedPlaceholder>> placeholders = new ArrayList<>();

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
            List<MatchedPlaceholder> placeholders = Holograms.getInstance().getPlaceholderAdapter().matchPlaceholders(line);

            for (MatchedPlaceholder p : placeholders) {
                this.placeholderNames.add(p.name);
            }

            this.placeholders.add(placeholders);
        }
    }
}
