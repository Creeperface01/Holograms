package gt.creeperface.holograms.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import gt.creeperface.holograms.Holograms;
import gt.creeperface.holograms.entity.HologramEntity;
import gt.creeperface.holograms.util.Values;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CreeperFace
 */

@AllArgsConstructor
public class FormWindowManager {

    private Holograms plugin;

    /*public void addEditWindow(Player p, HologramEntity entity) {
        addEditWindow(p, entity, HologramEntity.getText(entity.getHologramId()), 0);
    }

    public void addEditWindow(Player p, HologramEntity entity, List<String> lines, int emptyLines) {
        FormWindowCustom window = new FormWindowCustom(entity.getHologramId());

        window.addElement(new ElementLabel("Position"));
        window.addElement(new ElementInput("X", "X", "" + entity.getX()));
        window.addElement(new ElementInput("Y", "Y", "" + entity.getY()));
        window.addElement(new ElementInput("Z", "Z", "" + entity.getZ()));

        window.addElement(new ElementLabel(""));
        window.addElement(new ElementLabel("Move"));
        window.addElement(new ElementInput("X", "X", "0"));
        window.addElement(new ElementInput("Y", "Y", "0"));
        window.addElement(new ElementInput("Z", "Z", "0"));

        window.addElement(new ElementLabel("Translations"));
        window.addElement(new ElementLabel("Translations"));

        boolean negativeCount = emptyLines < 0;
        int lineCount = 1;
        //List<String> lines = HologramEntity.getText(entity.getHologramId());
        if(lines.isEmpty()) {
            lines.add("");
        }

        for (String line : lines) {
            if (negativeCount) {
                emptyLines++;

                if (emptyLines >= 0) {
                    break;
                }
            }

            window.addElement(new ElementInput("", "line " + lineCount++, line.replaceAll("§", "&")));
        }

        if (!negativeCount) {
            for (int i = 0; i < emptyLines; i++) {
                window.addElement(new ElementInput("", "line " + lineCount++));
            }
        }

        window.addElement(new ElementLabel(""));
        //window.addElement(new ElementLabel("Add lines"));
        window.addElement(new ElementInput("Add lines", "count", "0"));
        window.addElement(new ElementToggle("Remove hologram", false));

        ((GTPlayer) p).showFormWindow(window, Values.WINDOW_ID);
    }*/

    public void addGeneralWindow(Player p, HologramEntity entity) {
        FormWindowCustom window = new FormWindowCustom(entity.getHologramId());

        window.addElement(new ElementLabel("Position"));
        window.addElement(new ElementInput("X", "X", "" + entity.getX()));
        window.addElement(new ElementInput("Y", "Y", "" + entity.getY()));
        window.addElement(new ElementInput("Z", "Z", "" + entity.getZ()));

        window.addElement(new ElementLabel(""));
        window.addElement(new ElementLabel("Move"));
        window.addElement(new ElementInput("X", "X", "0"));
        window.addElement(new ElementInput("Y", "Y", "0"));
        window.addElement(new ElementInput("Z", "Z", "0"));

        window.addElement(new ElementLabel(""));
        window.addElement(new ElementToggle("Remove hologram", false));

        window.addElement(new ElementInput("Autoupdate", "ticks", "" + entity.getHologram().getUpdateInterval()));

        p.showFormWindow(window, Values.GENERAL_WINDOW_ID);
    }

    public void addTextWindow(Player p, HologramEntity entity) {
        addTextWindow(p, entity, entity.getHologram().getRawTranslations());
    }

    public void addTextWindow(Player p, HologramEntity entity, List<List<String>> trans) {
        FormWindowCustom window = new FormWindowCustom(entity.getHologramId());

        window.addElement(new ElementLabel("Translations"));

        int lineCount = 1;
        //List<String> lines = HologramEntity.getText(entity.getHologramId());
        if (trans.isEmpty()) {
            trans.add(new ArrayList<>());
        }

        for (int i = 0; i < trans.size(); i++) {
            window.addElement(new ElementLabel("Translation: " + i));
            List<String> lines = trans.get(i);

            if (lines.isEmpty()) {
                lines.add("");
            }

            for (String line : lines) {
                window.addElement(new ElementInput("", "line " + lineCount++, line.replaceAll("§", "&")));
            }

            lineCount = 1;
        }

        window.addElement(new ElementLabel(""));
        window.addElement(new ElementLabel(""));
        window.addElement(new ElementInput("Add lines", "count", "0"));
        window.addElement(new ElementInput("Add translation", "count", "0"));

        p.showFormWindow(window, Values.TEXT_WINDOW_ID);
    }

    public void addMainWindow(Player p, HologramEntity entity) {
        FormWindowSimple window = new FormWindowSimple("Hologram Settings - (" + entity.getHologramId() + ")", "");
        window.addButton(new ElementButton("General settings"));
        window.addButton(new ElementButton("Text settings"));

        p.showFormWindow(window, Values.WINDOW_ID);
    }
}
