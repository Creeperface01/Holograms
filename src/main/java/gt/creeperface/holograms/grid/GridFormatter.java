package gt.creeperface.holograms.grid;

import cn.nukkit.utils.TextFormat;
import gt.creeperface.holograms.Hologram;
import gt.creeperface.holograms.HologramConfiguration;
import gt.creeperface.holograms.api.grid.source.GridSource;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author CreeperFace
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridFormatter {

    private static final int SPACE_WIDTH = 4;
    private static final double ASCII_SIZE_MULTIPLIER = 2;

    public static List<String> process(List<String> lines, Hologram.GridSettings settings) {
        List<List<String>> split = new ArrayList<>();
        Int2ObjectMap<String> excluded = new Int2ObjectOpenHashMap<>();
        boolean source = settings.getSource() != null;

        boolean[] unicode = new boolean[lines.size()];

        int excludedIndex = 0;
        for (String line : lines) {
            excludedIndex++;

            if (source || line.startsWith(HologramConfiguration.getGridExcluder())) {
                excluded.put(excludedIndex - 1, line);
                continue;
            }

            if (CharactersTable.isUnicode(line)) {
                unicode[excludedIndex - 1] = true;
            }

            String[] splitLine = line.split(HologramConfiguration.getGridColSeparator());

            split.add(Arrays.asList(splitLine));
        }

        if (source) {
            return processSource(split, excluded, unicode, settings);
        } else {
            return process(split, excluded, unicode, settings);
        }
    }

    private static List<String> processSource(List<List<String>> lines, Int2ObjectMap<String> excluded, boolean[] unicode, Hologram.GridSettings settings) {
        GridSource source = settings.getSource();

        if (settings.isHeader() && source.supportsHeader()) {
            lines.add(source.getHeader());
            lines.add(Collections.emptyList());
        }

        source.resetOffset();
        while (source.hasNextRow()) {
            lines.add(source.nextRow());
        }

        boolean[] unicode2 = new boolean[lines.size()];
        System.arraycopy(unicode, 0, unicode2, 0, unicode.length);

        for (int i = unicode.length; i < unicode2.length; i++) {
            unicode2[i] = CharactersTable.isUnicode(String.join("", lines.get(i)));
        }

        return process(lines, excluded, unicode2, settings);
    }

    private static List<String> process(List<List<String>> lines, Int2ObjectMap<String> excluded, boolean[] unicode, Hologram.GridSettings settings) {
        int columnCount = 0;

        List<ColumnEntry[]> lineCols = new ArrayList<>(lines.size());
        List<Integer> maxColumnsLengths = new ArrayList<>();

        for (int li = 0; li < lines.size(); li++) {
            List<String> line = lines.get(li);
            final int index = li;

            //get lengths of separate columns
            ColumnEntry[] cols = line.stream().map(column -> {
                int len = 0;
                boolean wasColor = false;
                boolean bold = false;
                boolean unicodeLine = unicode[index];

                for (char c : column.toCharArray()) {
                    if (c == '§') {
                        wasColor = true;
                        continue;
                    }

                    if (wasColor) {
                        wasColor = false;

                        if (c == 'l' || c == 'L') { //bold
                            bold = true;
                        } else if (c == 'r' || c == 'R') { //reset
                            bold = false;
                        } else if (TextFormat.getByChar(c) != null) {
                            continue;
                        } else {
                            len += CharactersTable.lengthOf('§', unicodeLine);
                        }
                    }

                    if (bold) {
                        len++;
                    }

                    len += CharactersTable.lengthOf(c, unicodeLine);
                }

                return new ColumnEntry(column + "§r", len);
            }).toArray(ColumnEntry[]::new);

            //check max column count
            if (cols.length > columnCount) {
                columnCount = cols.length;
            }

            //max length per column
            for (int i = 0; i < cols.length; i++) {
                ColumnEntry col = cols[i];

                if (maxColumnsLengths.size() <= i) {
                    maxColumnsLengths.add(col.length);
                    continue;
                }

                int maxLength = maxColumnsLengths.get(i);

                if (col.length > maxLength) {
                    maxColumnsLengths.set(i, col.length);
                }
            }

            lineCols.add(cols);
        }

        List<String> newLines = new ArrayList<>();
        int excludedIndex = 0;

        char[] columnSpaces = new char[settings.getColumnSpace() / SPACE_WIDTH];
        Arrays.fill(columnSpaces, ' ');

        //format columns
        for (int ci = 0; ci < lineCols.size(); ci++) {
            ColumnEntry[] lineCol = lineCols.get(ci);

            String excludedLine;

            while ((excludedLine = excluded.get(excludedIndex++)) != null) {
                newLines.add(excludedLine);
            }

            StringBuilder lineBuilder = new StringBuilder();
            double diff = 0;

            for (int i = 0; i < lineCol.length; i++) {
                ColumnEntry entry = lineCol[i];

                int maxLength = maxColumnsLengths.get(i);

                //length to add
                double expectedLength = (maxLength - entry.length) + diff;

                //convert length to spaces
                int spaces = (int) Math.round(expectedLength / SPACE_WIDTH);

                //space chars to append
                char[] spaceChars = new char[spaces == 1 && diff > 0 ? 1 : spaces / 2];
                Arrays.fill(spaceChars, ' ');

                //save diff after rounding by space length
                diff = (expectedLength - (spaces * SPACE_WIDTH)) + ((spaces % 2) * SPACE_WIDTH);

                lineBuilder.append(spaceChars);
                lineBuilder.append(entry.column);
                lineBuilder.append(spaceChars);

                lineBuilder.append(columnSpaces);
            }

            String line = lineBuilder.toString();

            if (!line.isEmpty()) {
                line = line.substring(0, line.length() - (settings.getColumnSpace() / SPACE_WIDTH));
            }

            newLines.add(line);
        }

        return newLines;
    }

    @AllArgsConstructor
    @Getter
    @ToString
    private static class ColumnEntry {

        private final String column;
        private final int length;
    }
}
