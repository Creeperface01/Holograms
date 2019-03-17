package gt.creeperface.holograms.sql;

import ru.nukkit.dblib.DbLib;

import java.sql.Connection;

/**
 * @author CreeperFace
 */
public final class MySQLManager {

    public static Connection createConnection() {
        return DbLib.getDefaultConnection();
    }
}
