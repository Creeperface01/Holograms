package gt.creeperface.holograms.grid.source;

import lombok.RequiredArgsConstructor;

import java.util.*;

public abstract class MapGridSource extends AbstractGridSource<String> {

    private Map<String, Entry> entries;

    public MapGridSource(SourceParameters parameters) {
        super(parameters);
    }

    protected void initColumns(List<String> columns) {
        this.entries = new HashMap<>();

        for (String column : columns) {
            String[] split = column.split("\\.");

            Map<String, Entry> current = this.entries;
            for (int i = 0; i < split.length; i++) {
                String section = split[i];

                Entry existing = current.get(section);
                if (existing == null) {
                    existing = new Entry(section);
                    current.put(section, existing);
                }

                if (i < split.length - 1) {
                    if (existing.subEntries == null) {
                        existing.subEntries = new HashMap<>();
                    }

                    current = existing.subEntries;
                }
            }
        }
    }

    protected List<List<String>> parse(Map<String, Object> data, int limit, int offset) {
        List<List<String>> result = new ArrayList<>(Math.min(50, limit));

//        for (Entry entry : ) {
//
//        }
        return Collections.emptyList();
    }

    private List<String> parseEntry(Entry entry, Map<String, Object> data) {
        return Collections.emptyList();
    }

    @RequiredArgsConstructor
    private class Entry {

        private final String key;
        private Map<String, Entry> subEntries;

    }
}
