package gt.creeperface.holograms.grid.source;

import cn.nukkit.utils.MainLogger;
import com.google.common.base.Preconditions;
import gt.creeperface.holograms.sql.MySQLManager;
import lombok.Cleanup;

import java.sql.*;
import java.util.*;

/**
 * @author CreeperFace
 */
public class MySQLGridSource extends AbstractGridSource<String> {

    private final String table;
    private Set<String> columns;

    public MySQLGridSource(SourceParameters parameters, Map<String, Object> data) {
        super(parameters);
        Preconditions.checkNotNull(data.get("table"));

        this.table = Objects.toString(data.get("table"));

        if (data.containsKey("columns")) {
            this.columns = new HashSet<>((List) data.get("columns"));
        }
    }

    @Override
    public void load(int offset, int limit) {
        try (Connection con = MySQLManager.createConnection()) {
            @Cleanup PreparedStatement statement = con.prepareStatement("SELECT * FROM " + table + " LIMIT ? OFFSET ?");
            statement.setInt(1, limit);
            statement.setInt(2, offset);

            @Cleanup ResultSet result = statement.executeQuery();

            List<String> colNames = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            ResultSetMetaData meta = result.getMetaData();

            for (int i = 0; i < meta.getColumnCount(); i++) {
                String colName = meta.getColumnName(i + 1);

                if (loadColumn(colName)) {
                    colNames.add(colName);
                }
            }

            while (result.next()) {
                List<String> row = new ArrayList<>();

                for (String colName : colNames) {
                    row.add(Objects.toString(result.getObject(colName)));
                }

                rows.add(row);
            }

            this.setHeader(colNames);
            this.load(rows);
        } catch (SQLException e) {
            MainLogger.getLogger().logException(e);
        }
    }

    private boolean loadColumn(String column) {
        return this.columns == null || this.columns.contains(column);
    }

    @Override
    public boolean supportsHeader() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "mysql";
    }

    @Override
    public CallType getAllowedCallType() {
        return CallType.ASYNC;
    }
}
