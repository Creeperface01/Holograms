package gt.creeperface.holograms.api.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author CreeperFace
 */
public class Grid {

    protected int columnSpace = 20;
    protected int minColumnSpace = -1;
    protected int maxColumnSpace = -1;

    protected String columnSeparator = ";";

    public List<String> process(List<String> lines) {
        int columns = 0;

        List<List<ColumnEntry>> lineCols = new ArrayList<>(6);
        List<Integer> columnsLengths = new ArrayList<>();


        for (String line : lines) {
            String[] split = line.split(columnSeparator);

            if (split.length <= 0) {
                continue;
            }

            ColumnEntry[] cols = Arrays.stream(split).map(s -> {
                int len = 0;

                for (char c : s.toCharArray()) {
                    len += CharactersTable.lengthOf(c);
                }

                return new ColumnEntry(s, len);
            }).toArray(ColumnEntry[]::new);

            if (cols.length > columns) {
                columns = cols.length;
            }

            for (int i = 0; i < cols.length; i++) {
                ColumnEntry col = cols[i];

                if (columnsLengths.size() <= i) {
                    columnsLengths.add(col.length);
                }

                int maxLength = columnsLengths.get(i);

                if (col.length > maxLength) {
                    columnsLengths.set(i, col.length);
                }
            }

            lineCols.add(Arrays.asList(cols));
        }

        List<String> newLines = new ArrayList<>();

        for (List<ColumnEntry> lineCol : lineCols) {
            StringBuilder lineBuilder = new StringBuilder();
            int diff = 0;

            for (int i = 0; i < lineCol.size(); i++) {
                ColumnEntry entry = lineCol.get(i);

                lineBuilder.append(entry.column);


                int maxLength = columnsLengths.get(i);
                int expectedLength = (maxLength - entry.column.length()) + diff;

                int spaces = (int) Math.round((double) expectedLength / 4);
                diff = expectedLength - spaces;

                char[] spaceChars = new char[spaces];
                Arrays.fill(spaceChars, ' ');

                lineBuilder.append(spaceChars);
            }

            newLines.add(lineBuilder.toString());
        }

        return newLines;
    }

    @AllArgsConstructor
    @Getter
    private static class ColumnEntry {

        private final String column;
        private final int length;
    }
}
