package gt.creeperface.holograms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author CreeperFace
 */
public class HologramPage {

    private final List<HologramTranslation> translations = new ArrayList<>();

    public HologramPage(List<List<String>> trans) {
        for (List<String> s : trans) {
            translations.add(new HologramTranslation(s));
        }
    }

    public HologramTranslation getTranslation(int index) {
        return translations.get(index);
    }

    public void addTranslation(HologramTranslation... translations) {
        this.translations.addAll(Arrays.asList(translations));
    }

    public void removeTranslation(int count) {

    }

    public void setTranslations(Collection<HologramTranslation> translations) {

    }
}
