package gt.creeperface.holograms.grid.source;

import gt.creeperface.holograms.Holograms;
import gt.creeperface.holograms.api.placeholder.PlaceholderAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author CreeperFace
 */
public class PlaceholderGridSource extends AbstractGridSource<Object> {

    private final PlaceholderAdapter placeholderAdapter;
    private final String placeholder;

    public PlaceholderGridSource(SourceParameters parameters, Holograms plugin, Map<String, Object> data) {
        super(parameters);
        placeholderAdapter = plugin.getPlaceholderAdapter();
        this.placeholder = (String) data.get("placeholder");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(int offset, int limit) {
        if (!this.placeholderAdapter.supports()) {
            return;
        }

        Object value = placeholderAdapter.getValue(placeholder);

        if (!(value instanceof List)) {
            return;
        }

        List<List<Object>> source = new ArrayList<>();

        ((List) value).forEach(columns -> {
            if (columns instanceof List) {
                source.add((List) ((List) columns).stream().map(Object::toString).collect(Collectors.toList()));
            } else {
                source.add(Collections.singletonList(columns.toString()));
            }
        });

        super.load(source);
    }

    @Override
    public String getIdentifier() {
        return "placeholder";
    }
}
