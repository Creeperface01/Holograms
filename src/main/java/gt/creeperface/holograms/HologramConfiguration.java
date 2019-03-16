package gt.creeperface.holograms;

import cn.nukkit.utils.SimpleConfig;
import lombok.Getter;

/**
 * @author CreeperFace
 */

@Getter
public class HologramConfiguration extends SimpleConfig {

    public static final int VERSION = 2;

    @Path("lines_gaps")
    private double linesGaps;

    @Path("async_batch")
    private boolean asyncBatch;

    @Path("save_interval")
    private int saveInterval;

    @Path("grid.col_separator")
    private String gridColumnSeparator;

    @Path("grid.excluder")
    private String gridRowExcluder;

    @Getter
    @Skip
    private static double lineGap;

    @Getter
    private static String gridColSeparator;

    @Getter
    private static String gridExcluder;

    HologramConfiguration(Holograms plugin) {
        super(plugin, "config.yml");
        this.load();

        lineGap = linesGaps;
        gridColSeparator = gridColumnSeparator;
        gridExcluder = gridRowExcluder;
    }
}
