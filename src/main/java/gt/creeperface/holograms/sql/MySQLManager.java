package gt.creeperface.holograms.sql;

import cn.nukkit.utils.MainLogger;
//import ru.nukkit.dblib.DbLib;
//import ru.nukkit.dblib.core.M;
//import ru.nukkit.dblib.nukkit.ConfigNukkit;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.TimeZone;

/**
 * @author CreeperFace
 */
public final class MySQLManager {
/*
    private static ConfigNukkit config;

    static {
        M.setDebugMode(true);

        try {
            Field cfg = DbLib.class.getDeclaredField("config");
            cfg.setAccessible(true);

            config = (ConfigNukkit) cfg.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            MainLogger.getLogger().logException(e);
        }
    }

    public static Connection createConnection() {
        return getMySqlConnection(config.dbMySqlUrl(), config.dbMySqlPort(), config.dbMySqlDatabase(), config.dbMySqlUsername(), config.dbMySqlPassword());
    }

    public static Connection getMySqlConnection(String host, int port, String database, String user, String password) {
        StringBuilder sb = new StringBuilder(host);
        if (port >= 0) {
            sb.append(":").append(port);
        }

        sb.append("/").append(database);
        sb.append("?useSSL=false");
        sb.append("&serverTimezone=");
        sb.append(TimeZone.getDefault().getID());

        return getMySqlConnection(sb.toString(), user, password);
    }

    private static Connection getMySqlConnection(String url, String user, String password) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(url.startsWith("jdbc:mysql://") ? url : "jdbc:mysql://" + url, user, password);
        } catch (Exception e) {
            M.debugException(e);
            return null;
        }
    }*/
}
