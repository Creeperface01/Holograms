package gt.creeperface.holograms.api.grid.source;

import java.util.List;

/**
 * @author CreeperFace
 */
public interface GridSource<T extends Object> {

    String getIdentifier();

    String getName();

    int getOffset();

    int getLimit();

    boolean hasNextRow();

    List<T> nextRow();

    boolean hasNextColumn();

    String nextColumn();

    default void load() {
        load(getOffset(), getLimit());
    }

    void load(int offset, int limit);

    int getRows();

    void forceReload();

    void resetOffset();

    default boolean supportsHeader() {
        return false;
    }

    default List<String> getHeader() {
        throw new UnsupportedOperationException("GridSource " + getName() + " doesn't support header");
    }

    default CallType getAllowedCallType() {
        return CallType.SYNC;
    }

    enum CallType {
        SYNC,
        ASYNC,
        ANY
    }
}
