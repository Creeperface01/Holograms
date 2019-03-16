package gt.creeperface.holograms.grid;

import cn.nukkit.utils.TextFormat;
import gt.creeperface.holograms.Hologram;
import gt.creeperface.holograms.HologramConfiguration;
import gt.creeperface.holograms.api.grid.source.GridSource;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author CreeperFace
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridFormatter {

    public static List<String> process(List<String> lines, Hologram.GridSettings settings) {
        List<List<String>> split = new ArrayList<>();
        Int2ObjectMap<String> excluded = new Int2ObjectOpenHashMap<>();
        boolean source = settings.getSource() != null;

        int excludedIndex = 0;
        for (String line : lines) {
            excludedIndex++;

            if (source || line.startsWith(HologramConfiguration.getGridExcluder())) {
                excluded.put(excludedIndex - 1, line);
                continue;
            }

            String[] splitLine = line.split(HologramConfiguration.getGridColSeparator());

            if (splitLine.length <= 0) {
                continue;
            }

            split.add(Arrays.asList(splitLine));
        }

        if (source) {
            return processSource(split, excluded, settings);
        } else {
            return process(split, excluded, settings);
        }
    }

    private static List<String> processSource(List<List<String>> lines, Int2ObjectMap<String> excluded, Hologram.GridSettings settings) {
        GridSource source = settings.getSource();

        if (settings.isHeader() && source.supportsHeader()) {
            lines.add(source.getHeader());
            lines.add(Collections.emptyList());
        }

        while (source.hasNextRow()) {
            lines.add(source.nextRow());
        }

        return process(lines, excluded, settings);
    }

    private static List<String> process(List<List<String>> lines, Int2ObjectMap<String> excluded, Hologram.GridSettings settings) {
        int columns = 0;

        List<List<ColumnEntry>> lineCols = new ArrayList<>(lines.size());
        List<Integer> maxColumnsLengths = new ArrayList<>();
//        List<Integer> minColumnsLengths = new ArrayList<>();

//        int li = 0; //excluded line count
        for (List<String> line : lines) {
//            li++;
//            if (line.startsWith(HologramConfiguration.getGridExcluder())) {
//                excluded.put(li - 1, line);
//                continue;
//            }
//
//            String[] split = line.split(HologramConfiguration.getGridColSeparator());
//
//            if (split.length <= 0) {
//                continue;
//            }

            ColumnEntry[] cols = line.stream().map(s -> {
                int len = 0;
                boolean wasColor = false;

                for (char c : s.toCharArray()) {
                    if (c == '§') {
                        wasColor = true;
                        continue;
                    }

                    if (wasColor) {
                        wasColor = false;

                        if (TextFormat.getByChar(c) != null) {
                            continue;
                        }
                    }

                    len += CharactersTable.lengthOf(c, false); //TODO: unicode
                }

                return new ColumnEntry(s, len);
            }).toArray(ColumnEntry[]::new);

            if (cols.length > columns) {
                columns = cols.length;
            }

            for (int i = 0; i < cols.length; i++) {
                ColumnEntry col = cols[i];

                if (maxColumnsLengths.size() <= i) {
                    maxColumnsLengths.add(col.length);
                }

//                if(minColumnsLengths.size() <= i) {
//                    minColumnsLengths.add(col.length);
//                }

                int maxLength = maxColumnsLengths.get(i);
//                int minLength = minColumnsLengths.get(i);

                if (col.length > maxLength) {
                    maxColumnsLengths.set(i, col.length);
                }

//                if (col.length < minLength) {
//                    minColumnsLengths.set(i, col.length);
//                }
            }

            lineCols.add(Arrays.asList(cols));
        }

//        MainLogger.getLogger().info("max lengths: "+maxColumnsLengths);

        List<String> newLines = new ArrayList<>();

        int excludedIndex = 0;
        for (List<ColumnEntry> lineCol : lineCols) {
            String excludedLine;
            boolean _break = false;

            while ((excludedLine = excluded.get(excludedIndex++)) != null) {
                newLines.add(excludedLine);
                _break = true;
            }
            if (_break) break;

            StringBuilder lineBuilder = new StringBuilder();
            int diff = 0;

            for (int i = 0; i < lineCol.size(); i++) {
                ColumnEntry entry = lineCol.get(i);
                if (entry.column.isEmpty()) {
                    continue;
                }

                int maxLength = maxColumnsLengths.get(i);
//                int minLength = minColumnsLengths.get(i);

                int expectedLength = (maxLength - entry.length) + diff;

                int spaces = (int) Math.round((double) expectedLength / 4); //space is 4
                diff = expectedLength - (spaces * 4) + (spaces % 2) * 4;

                char[] spaceChars = new char[spaces / 2];
//                MainLogger.getLogger().info("spaces: "+spaces+"  rounded: "+spaceChars.length);
                Arrays.fill(spaceChars, ' ');

//                MainLogger.getLogger().info("appended "+spaces+" spaces for "+entry.column);

                lineBuilder.append(spaceChars);
                lineBuilder.append(entry.column);
                lineBuilder.append(spaceChars);

//                int colSpaceLen = columnSpace;
//                diff += colSpaceLen % 4;

                char[] columnSpaces = new char[settings.getColumnSpace() / 4];
                Arrays.fill(columnSpaces, ' ');

                lineBuilder.append(columnSpaces);
            }

            newLines.add(lineBuilder.toString());
        }

//        int maxLength = 0;
//
//        for (String line : newLines) {
//            int len = line.length();
//            int i = len;
//
//            while(i > 0 && line.charAt(--i) != ' ') {
//                len--;
//            }
//
//            if(len > maxLength) {
//                maxLength = len;
//            }
//        }
//
        for (int i = 0; i < newLines.size(); i++) {
            String line = newLines.get(i);

            if (line.isEmpty()) {
                continue;
            }

            newLines.set(i, line.substring(0, line.length() - (settings.getColumnSpace() / 4)));
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
