package gt.creeperface.holograms;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author CreeperFace
 */

@AllArgsConstructor
public class HologramTranslation implements gt.creeperface.holograms.api.HologramTranslation {

    private List<String> lines;

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
}
