package gt.creeperface.holograms.grid.source;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CreeperFace
 */
public class ExampleGridSource extends AbstractGridSource<Object> {

    public ExampleGridSource(SourceParameters parameters) {
        super(parameters);
    }

    @Override
    public void load(int offset, int limit) {
        List<List<Object>> source = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            List<Object> column = new ArrayList<>();

            for (int j = 0; j < 5; j++) {
                column.add(Math.pow(2, j));
            }

            source.add(column);
        }

        this.load(source);
    }

    @Override
    public String getIdentifier() {
        return "example";
    }
}
