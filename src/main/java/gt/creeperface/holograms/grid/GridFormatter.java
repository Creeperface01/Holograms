package gt.creeperface.holograms.grid;

import cn.nukkit.utils.TextFormat;
import gt.creeperface.holograms.Hologram;
import gt.creeperface.holograms.Hologram.GridSettings.ColumnTemplate;
import gt.creeperface.holograms.HologramConfiguration;
import gt.creeperface.holograms.api.grid.source.GridSource;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.*;

import java.text.Normalizer;
import java.util.*;

/**
 * @author CreeperFace
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridFormatter {

    private static final int SPACE_WIDTH = 4;

    public static List<String> process(List<String> lines, Hologram.GridSettings settings) {
        List<List<String>> split = new ArrayList<>();
        Int2ObjectMap<String> excluded = new Int2ObjectOpenHashMap<>();
        boolean source = settings.getSource() != null;

        boolean[] unicode = new boolean[lines.size()];

        int excludedIndex = 0;
        boolean excluder = false;
        for (String line : lines) {
            excludedIndex++;

            if (source || (excluder = line.startsWith(HologramConfiguration.getGridExcluder()))) {
                excluded.put(excludedIndex - 1, excluder ? line.substring(HologramConfiguration.getGridExcluder().length()) : line);
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
        GridSource<Object> source = settings.getSource();
        source.startReading();

        try {
            if (settings.isHeader() && source.supportsHeader()) {
                lines.add(source.getHeader());
                lines.add(Collections.emptyList());
            }

            source.resetOffset();

            List<ColumnTemplate> templates = settings.getColumnTemplates();

            while (source.hasNextRow()) {
                List<Object> columns = source.nextRow();
                List<String> replaced = new ArrayList<>(columns.size());

                for (int i = 0; i < columns.size(); i++) {
                    String col = Objects.toString(columns.get(i));

                    if (settings.isNormalize()) {
                        col = Normalizer
                                .normalize(col, Normalizer.Form.NFD)
                                .replaceAll("[^\\p{ASCII}]", "");
                    }

                    ColumnTemplate template = templates.size() > i ? templates.get(i) : null;

                    if (template == null) {
                        replaced.add(col);
                        continue;
                    }

                    replaced.add(template.replace(col));
                }

                lines.add(replaced);
            }
        } finally {
            source.stopReading();
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
                boolean expectFormat = false;
                boolean bold = false;
                boolean unicodeLine = unicode[index];

                char[] chars = column.toCharArray();

                for (int j = 0; j < chars.length; j++) {
                    char c = chars[j];

                    if (c == '§') {
                        if (j == chars.length - 1) { //last char, isn't rendered
                            break;
                        }

                        expectFormat = true;
                        continue;
                    }

                    if (expectFormat) {
                        expectFormat = false;

                        if (c == 'l' || c == 'L') { //bold
                            bold = true;
                            continue;
                        } else if (c == 'r' || c == 'R') { //reset
                            bold = false;
                            continue;
                        } else if (TextFormat.getByChar(c) != null) {
                            continue;
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

//        for (ColumnEntry[] lineCol : lineCols) {
//            MainLogger.getLogger().info(Arrays.deepToString(lineCol));
//        }

        List<String> newLines = new ArrayList<>();
        int excludedIndex = 0;

        char[] columnSpaces = new char[settings.getColumnSpace() / SPACE_WIDTH];
        Arrays.fill(columnSpaces, ' ');

        String excludedLine;

        //format columns
        for (int ci = 0; ci < lineCols.size(); ci++) {
            ColumnEntry[] lineCol = lineCols.get(ci);

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
                char[] spaceChars = new char[spaces / 2];
                Arrays.fill(spaceChars, ' ');

                //(diff > 0 = the actual length is lower than expected) so we append one space to increase difference
                if (spaceChars.length == 0 && diff > 0) {
                    lineBuilder.append(' ');
                }

                //save diff after rounding by space length
                diff = (expectedLength - (spaces * SPACE_WIDTH)) + (spaces > 2 ? ((spaces % 2) * SPACE_WIDTH) : 0);

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

        while ((excludedLine = excluded.get(excludedIndex++)) != null) {
            newLines.add(excludedLine);
        }

//        for (String newLine : newLines) {
//            StringBuilder logBuilder = new StringBuilder();
//
//            boolean expectFormat = false;
//            boolean bold = false;
//
//            char[] chars = newLine.toCharArray();
//            for (int j = 0; j < chars.length; j++) {
//                char c = chars[j];
//
//                if (c == '§') {
//                    if (j == chars.length - 1) { //last char, isn't rendered
//                        break;
//                    }
//
//                    expectFormat = true;
//                    continue;
//                }
//
//                if (expectFormat) {
//                    expectFormat = false;
//
//                    if (c == 'l' || c == 'L') { //bold
//                        bold = true;
//                        continue;
//                    } else if (c == 'r' || c == 'R') { //reset
//                        bold = false;
//                        continue;
//                    } else if (TextFormat.getByChar(c) != null) {
//                        continue;
//                    }
//                }
//
//                int len = CharactersTable.lengthOf(c, false);
//
//                if (bold) {
//                    len++;
//                }
//
//                char[] chrs = new char[len];
//                Arrays.fill(chrs, c);
//
//                logBuilder.append(chrs);
//            }
//
//            MainLogger.getLogger().info(logBuilder.toString());
//        }

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
