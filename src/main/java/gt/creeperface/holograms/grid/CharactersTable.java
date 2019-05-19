package gt.creeperface.holograms.grid;

import cn.nukkit.utils.MainLogger;
import gt.creeperface.holograms.util.Utils;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author CreeperFace
 */
@UtilityClass
public final class CharactersTable {

    private static final byte[] gliphsWidth = new byte[65536];

    private static final int[] asciiWidth = new int[65536];

    //    private static final Map<Character, Byte> charMap = new HashMap<>();
//
    static {
        Arrays.fill(asciiWidth, -100);

        byte[] charLengths = new byte[]{
                4, 2, 5, 6, 6, 6, 6, 3, 5, 5, 5, 6, 2, 6, 2, 6,
                6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 2, 2, 5, 6, 5, 6,
                7, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6,
                6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 4, 6, 6,
                6, 6, 6, 6, 6, 5, 6, 6, 2, 6, 5, 3, 6, 6, 6, 6,
                6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 5, 2, 5, 7
        };

        char[] chars = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_abcdefghijklmnopqrstuvwxyz{|}~".toCharArray();

        for (int i = 0; i < chars.length; i++) {
            asciiWidth[chars[i]] = charLengths[i];
        }

        asciiWidth['§'] = 6;
    }

//    public static void register(char c, byte length) {
//        charMap.put(c, length);
//    }
//
//    public static void register(Map<Character, Byte> map) {
//        charMap.putAll(map);
//    }
//
//    public static void register(Char2ByteMap map) {
//        charMap.putAll(map);
//    }
//

//    public static byte getOldWidth(char c) {
//        return charMap.getOrDefault(c, (byte) 0);
//    }

    public static boolean isUnicode(String input) {
        for (char c : input.toCharArray()) {
            if (isUnicode(c)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isUnicode(char c) {
        return asciiWidth[c] == -100;
    }

    public static int lengthOf(char c, boolean unicode) {
        if (c == ' ') {
            return 4;
        }

        if (!unicode && c > 0) {
            int width = asciiWidth[c];

            if (width != -100) {
                return width;
            }
        }

        int width = gliphsWidth[c];

        if (width <= 0) {
            return 0;
        }

        return (((width & 0xF) + 1) - (width >>> 4)) / 2 + 1;
    }

    public static void init(InputStream ascii, InputStream gliphs) throws IOException {
        int[] map = new int[256];

        Utils.readFontTexture(ascii, map);
        gliphs.read(gliphsWidth);

        //convert ids

        char[] array = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000".toCharArray();

        for (int i = 0; i < array.length; i++) {
            char c = array[i];

            asciiWidth[c] = map[i];
        }

//        charMap.forEach((c, old) -> {
//            int width = lengthOf(c, false);
//
//            MainLogger.getLogger().info("checking: '"+c+"'");
//            if(width != old) {
//                MainLogger.getLogger().info("Character '"+c+"' length diff old: "+old+"  new: "+width);
//            }
//        });
    }

    public static void anal(String input) {
        StringBuilder builder = new StringBuilder();

        for (char c : input.toCharArray()) {
            int len = lengthOf(c, false);

            char[] chars = new char[len];
            Arrays.fill(chars, c);

            builder.append(chars);
//            builder.append('(');
//            builder.append(c);
//            builder.append(')');
//
//            builder.append('{');
//            builder.append(len);
//            builder.append('}');
//            builder.append(' ');
        }

        MainLogger.getLogger().info(builder.toString());
    }
}
