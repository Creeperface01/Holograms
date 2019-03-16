package gt.creeperface.holograms.sql;

import cn.nukkit.utils.ConfigSection;
import ru.nukkit.dblib.DbLib;

import java.sql.Connection;

/**
 * @author CreeperFace
 */
public final class MySQLManager {

    private static String host;
    private static int port;
    private static String database;
    private static String user;
    private static String password;

    private static boolean loaded;

    public static void init(ConfigSection cfg) {
        host = cfg.getString("host");
        port = cfg.getInt("port");
        database = cfg.getString("database");
        user = cfg.getString("user");
        password = cfg.getString("password");
    }

    public static Connection createConnection() {
        return DbLib.getMySqlConnection(host, port, database, user, password);
    }
}
