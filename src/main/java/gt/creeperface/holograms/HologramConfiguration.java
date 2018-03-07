package gt.creeperface.holograms;

import cn.nukkit.utils.SimpleConfig;
import lombok.Getter;

/**
 * @author CreeperFace
 */

@Getter
public class HologramConfiguration extends SimpleConfig {

    @Path("lines_gaps")
    private double linesGaps;

    @Path("async_batch")
    private boolean asyncBatch;

    @Path("save_interval")
    private int saveInterval;

    @Getter
    private static double lineGap;

    HologramConfiguration(Holograms plugin) {
        super(plugin, "config.yml");
        this.load();

        lineGap = linesGaps;
    }
}
