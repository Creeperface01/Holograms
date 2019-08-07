package gt.creeperface.holograms.grid.source;

import gt.creeperface.holograms.api.grid.source.GridSource;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author CreeperFace
 */
public class LoadedGridSource extends AbstractGridSource<Object> {

    public LoadedGridSource(GridSource<Object> baseSource) {
        super(new SourceParameters());
        this.load(
                baseSource instanceof AbstractGridSource ?
                        new ArrayList<>(((AbstractGridSource<Object>) baseSource).getSource()) :
                        Collections.emptyList()
        );
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public void load(int offset, int limit) {

    }
}
