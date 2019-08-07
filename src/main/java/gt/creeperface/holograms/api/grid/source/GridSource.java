package gt.creeperface.holograms.api.grid.source;

import cn.nukkit.Server;
import cn.nukkit.scheduler.AsyncTask;
import gt.creeperface.holograms.Hologram.GridSettings.ColumnTemplate;
import gt.creeperface.holograms.Holograms;
import gt.creeperface.holograms.util.Values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author CreeperFace
 */
public interface GridSource<T> {

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

    void startReading();

    void stopReading();

    default boolean supportsHeader() {
        return false;
    }

    default List<String> getHeader() {
        throw new UnsupportedOperationException("GridSource " + getName() + " doesn't support header");
    }

    default boolean isLoaded() {
        return getRows() > 0;
    }

    default CallType getAllowedCallType() {
        return CallType.SYNC;
    }

    default CompletableFuture<List<ColumnTemplate>> prepareColumnTemplates() {
        CompletableFuture<List<ColumnTemplate>> future = new CompletableFuture<>();

        Supplier<List<ColumnTemplate>> task = () -> {
            load();

            if (!isLoaded()) {
                return null;
            } else {
                startReading();

                try {
                    int columnCount = 0;

                    while (hasNextRow()) {
                        int rowSize = nextRow().size();

                        if (rowSize > columnCount) {
                            columnCount = rowSize;
                        }
                    }

                    List<ColumnTemplate> templates = new ArrayList<>(columnCount);
                    List<String> nameMap = Collections.emptyList();


                    if (supportsHeader() && this.getHeader() != null) {
                        nameMap = this.getHeader();
                    }

                    for (int i = 0; i < columnCount; i++) {
                        ColumnTemplate template = new ColumnTemplate(
                                nameMap.size() > i ? nameMap.get(i) : null,
                                Values.COLUMN_TEMPLATE_PLACEHOLDER,
                                0,
                                Values.COLUMN_TEMPLATE_PLACEHOLDER.length()
                        );

                        templates.add(template);
                    }

                    return templates;
                } finally {
                    stopReading();
                }
            }
        };

        if (getAllowedCallType() == GridSource.CallType.SYNC) {
            List<ColumnTemplate> result = task.get();

            if (result == null) {
                future.completeExceptionally(new RuntimeException("Grid source has empty data"));
            } else {
                future.complete(result);
            }
        } else {
            Server.getInstance().getScheduler().scheduleAsyncTask(Holograms.getInstance(), new AsyncTask() {

                private List<ColumnTemplate> result;

                @Override
                public void onRun() {
                    result = task.get();
                }

                @Override
                public void onCompletion(Server server) {
                    if (result == null) {
                        future.completeExceptionally(new RuntimeException("Grid source has empty data"));
                    } else {
                        future.complete(result);
                    }
                }
            });
        }

        return future;
    }

    enum CallType {
        SYNC,
        ASYNC,
        ANY
    }
}
