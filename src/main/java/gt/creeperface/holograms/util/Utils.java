package gt.creeperface.holograms.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author CreeperFace
 */
@UtilityClass
public class Utils {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(.+?)%");

    public static List<String> mapPlaceholders(String input) {
        List<String> placeholders = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);

        while (matcher.find()) {
            String p = matcher.group();
            p = p.substring(1, p.length() - 1); //remove %

            placeholders.add(p);
        }

        return placeholders;
    }
}
