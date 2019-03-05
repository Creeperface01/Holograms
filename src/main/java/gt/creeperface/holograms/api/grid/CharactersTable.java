package gt.creeperface.holograms.api.grid;

import java.util.HashMap;
import java.util.Map;

/**
 * @author CreeperFace
 */
public final class CharactersTable {

    private CharactersTable() {
    }

    private static final Map<Character, Byte> charMap = new HashMap<>();

    static {

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
            charMap.put(chars[i], charLengths[i]);
        }
    }

    public static void register(char c, byte length) {
        charMap.put(c, length);
    }

    public static void register(Map<Character, Byte> map) {
        charMap.putAll(map);
    }

    public static byte lengthOf(char c) {
        return charMap.get(c);
    }
}
