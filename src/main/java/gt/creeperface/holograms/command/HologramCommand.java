package gt.creeperface.holograms.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.TextFormat;
import gt.creeperface.holograms.Hologram;
import gt.creeperface.holograms.Holograms;
import gt.creeperface.holograms.entity.HologramEntity;

import java.util.ArrayList;

/**
 * @author CreeperFace
 */
public class HologramCommand extends Command {

    private Holograms plugin;

    public HologramCommand(Holograms plugin) {
        super("hologram", "", "/hologram <hologram id | action>");
        this.setPermission("hologram.use");

        this.plugin = plugin;

        this.commandParameters.clear();
        this.commandParameters.put("default",
                new CommandParameter[]{
                        new CommandParameter("hologram id", CommandParamType.STRING, false)
                });
        this.commandParameters.put("remove",
                new CommandParameter[]{
                        new CommandParameter("action", false, new String[]{"edit", "update"})
                });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender)) {
            return false;
        }

        if (args.length <= 0) {
            return false;
        }

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;

        switch (args[0].toLowerCase()) {
            case "edit":
                HologramEntity entity = plugin.findNearEntity(p);

                if (entity == null || entity.closed) {
                    p.sendMessage(TextFormat.RED + "Entity not found");
                    return true;
                }

                plugin.editors.put(p.getId(), entity);
                plugin.getManager().addMainWindow(p, entity);
                break;
            case "update":
                this.plugin.getServer().getScheduler().scheduleAsyncTask(new AsyncTask() {
                    @Override
                    public void onRun() {
                        plugin.reloadHolograms();
                    }
                });
                break;
            default:
                String hologramId = args[0].toLowerCase();

                CompoundTag nbt = new CompoundTag()
                        .putList(new ListTag<>("Pos")
                                .add(new DoubleTag("0", ((Player) sender).x))
                                .add(new DoubleTag("1", ((Player) sender).y))
                                .add(new DoubleTag("2", ((Player) sender).z)))
                        .putList(new ListTag<DoubleTag>("Motion")
                                .add(new DoubleTag("0", 0))
                                .add(new DoubleTag("1", 0))
                                .add(new DoubleTag("2", 0)))
                        .putList(new ListTag<FloatTag>("Rotation")
                                .add(new FloatTag("0", (float) ((Player) sender).getYaw()))
                                .add(new FloatTag("1", (float) ((Player) sender).getPitch())))
                        .putString("hologramId", hologramId);

                plugin.getInternalHolograms().putIfAbsent(hologramId, new Hologram(hologramId, new ArrayList<>()));

                entity = new HologramEntity(((Player) sender).chunk, nbt);
                entity.spawnToAll();

                plugin.editors.put(p.getId(), entity);
                plugin.getManager().addMainWindow(p, entity);
                break;
        }
        return false;
    }
}
