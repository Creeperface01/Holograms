package gt.creeperface.holograms.form;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.utils.TextFormat;
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
public class FormWindowHandler {

    private Holograms plugin;

    public void handleResponse(Player p, int id, FormResponse response) {
        if (response == null) {
            plugin.editors.remove(p.getId());
            return;
        }

        if (id >= Values.WINDOW_ID && id <= Values.TEXT_WINDOW_ID) {
            HologramEntity entity = plugin.editors.get(p.getId());

            if (entity == null || entity.closed) {
                p.sendMessage(TextFormat.RED + "Entity doesn't exist");
                return;
            }

            switch (id) {
                case Values.WINDOW_ID:
                    handleMainResponse(p, entity, (FormResponseSimple) response);
                    break;
                case Values.GENERAL_WINDOW_ID:
                    handleGeneralResponse(p, entity, (FormResponseCustom) response);
                    break;
                case Values.TEXT_WINDOW_ID:
                    handleTextResponse(p, entity, (FormResponseCustom) response);
                    break;
            }
        }

        /*Map<Integer, String> inputs = Reflect.on(response).get("inputResponses");

        List<String> lines = new ArrayList<>();
        int i = 10;
        for (int count = 0; count < inputs.size() - 6; i++, count++) {
            String line = inputs.get(i);
            if(line == null) {
                break;
            }

            lines.add(line.replaceAll("&", "§"));
        }

        String addLines = inputs.get(i + 1);
        int add = getInt(addLines, 0);

        //Map<Integer, Boolean> toggles = Reflect.on(response).get("toggleResponses");
        //MainLogger.getLogger().info("i: "+i+"    toggle index: "+toggles.keySet().toString());


        if (add != 0) {
            plugin.getManager().addEditWindow(p, entity, lines, add);
            return;
        }

        Holograms.update(entity.getHologramId(), lines);*/
    }

    private void handleMainResponse(Player p, HologramEntity entity, FormResponseSimple response) {
        switch (response.getClickedButtonId()) {
            case 0:
                plugin.getManager().addGeneralWindow(p, entity);
                break;
            case 1:
                plugin.getManager().addTextWindow(p, entity);
                break;
        }
    }

    private void handleGeneralResponse(Player p, HologramEntity entity, FormResponseCustom response) {
        double x = getDouble(response.getInputResponse(1), entity.x);
        double y = getDouble(response.getInputResponse(2), entity.y);
        double z = getDouble(response.getInputResponse(3), entity.z);

        double offsetX = getDouble(response.getInputResponse(6), 0);
        double offsetY = getDouble(response.getInputResponse(7), 0);
        double offsetZ = getDouble(response.getInputResponse(8), 0);

        boolean remove = response.getToggleResponse(10);

        int updateInterval = getInt(response.getInputResponse(11), entity.getHologram().getUpdateInterval());
        entity.getHologram().setUpdateInterval(updateInterval);

        if (remove) {
            entity.close();
            p.sendMessage(TextFormat.GREEN + "Hologram removed");

            plugin.editors.remove(p.getId());
            return;
        }

        if (x != entity.x || y != entity.y || z != entity.z) {
            entity.moveTo(x, y, z);
            p.sendMessage(TextFormat.GREEN + "Hologram moved");
            plugin.editors.remove(p.getId());
        } else if (offsetX != 0 || offsetY != 0 || offsetZ != 0) {
            entity.offsetTo(offsetX, offsetY, offsetZ);
            p.sendMessage(TextFormat.GREEN + "Hologram moved");
            plugin.editors.remove(p.getId());
        }
    }

    private void handleTextResponse(Player p, HologramEntity entity, FormResponseCustom response) {
        List<List<String>> trans = new ArrayList<>();

        int i = 2;
        boolean lastLabel = false;

        List<String> lines = new ArrayList<>();
        while (true) {
            String r = response.getInputResponse(i++);

            if (r != null) {
                lines.add(r.replaceAll("&", "§"));
                lastLabel = false;
            } else {
                if (lastLabel) {
                    break;
                }

                trans.add(lines);
                lines = new ArrayList<>();
                lastLabel = true;
            }
        }

        boolean change = false;

        boolean negative;
        int addLines = getInt(response.getInputResponse(i), 0);
        if (addLines != 0) {
            negative = addLines < 0;
            addLines = Math.abs(addLines);

            while (addLines > 0) {
                for (List<String> list : trans) {
                    if (negative) {
                        list.remove(list.size() - 1);
                    } else {
                        list.add("");
                    }

                    change = true;
                }
                addLines--;
            }
        }

        int addTrans = getInt(response.getInputResponse(++i), 0);
        if (addTrans != 0) {
            negative = addTrans < 0;
            if (trans.size() + addTrans >= 1) {
                addTrans = Math.abs(addTrans);

                while (addTrans > 0) {
                    if (negative) {
                        trans.remove(trans.size() - 1);
                    } else {
                        trans.add(trans.get(0));
                    }

                    change = true;
                    addTrans--;
                }
            }
        }

        if (change) {
            plugin.getManager().addTextWindow(p, entity, trans);
        } else {
            plugin.update(entity.getHologramId(), trans);
        }
    }

    private double getDouble(String i, double defaultValue) {
        try {
            return Double.parseDouble(i);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getInt(String i, int defaultValue) {
        try {
            return Integer.parseInt(i);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
