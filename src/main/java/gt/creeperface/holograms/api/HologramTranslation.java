package gt.creeperface.holograms.api;

import java.util.List;

/**
 * Stores lines of certain translation
 *
 * @author CreeperFace
 */
public interface HologramTranslation {

    /**
     * Get line with specified index
     *
     * @param index line index
     * @return line of specified index
     */
    String getLine(int index);

    /**
     * Get how many lines translation has
     *
     * @return translation lines length
     */
    int getLineCount();

    /**
     * @return {@link List} of all translation lines
     */
    List<String> getLines();
}
