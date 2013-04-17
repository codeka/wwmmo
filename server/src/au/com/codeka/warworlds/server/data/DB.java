package au.com.codeka.warworlds.server.data;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This is a wrapper class that helps us with connecting to the database.
 */
public class DB {
    private static String sConnUrl;
    private static String sConnUser;
    private static String sConnPass;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // TODO: should never happen!
        }

        sConnUrl = "jdbc:mysql://localhost:3306/wwmmo";
        sConnUser = "wwmmo_user";
        sConnPass = "H98765gf!s876#Hdf2%7f";
    }

    public static SqlStmt prepare(String sql) throws SQLException {
        Connection conn = DriverManager.getConnection(sConnUrl, sConnUser, sConnPass);
        return new SqlStmt(conn, conn.prepareStatement(sql));
    }

    public static SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
        Connection conn = DriverManager.getConnection(sConnUrl, sConnUser, sConnPass);
        return new SqlStmt(conn, conn.prepareStatement(sql, autoGenerateKeys));
    }
}
