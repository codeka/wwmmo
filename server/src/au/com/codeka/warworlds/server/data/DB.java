package au.com.codeka.warworlds.server.data;

import java.sql.Connection;
import java.sql.SQLException;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

/**
 * This is a wrapper class that helps us with connecting to the database.
 */
public class DB {
    private static BoneCP sConnPool;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // TODO: should never happen!
        }

        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/wwmmo?useUnicode=true&characterEncoding=utf-8");
        config.setUsername("wwmmo_user");
        config.setPassword("H98765gf!s876#Hdf2%7f");
        config.setPartitionCount(4);
        config.setMaxConnectionsPerPartition(20);
        config.setConnectionTimeoutInMs(10000);
        config.setReleaseHelperThreads(0);
        config.setStatementReleaseHelperThreads(0);
        try {
            sConnPool = new BoneCP(config);
        } catch (SQLException e) {
            // TODO: should never happen!
        }
    }

    public static SqlStmt prepare(String sql) throws SQLException {
        Connection conn = sConnPool.getConnection();
        return new SqlStmt(conn, sql, conn.prepareStatement(sql), true);
    }

    public static SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
        Connection conn = sConnPool.getConnection();
        return new SqlStmt(conn, sql, conn.prepareStatement(sql, autoGenerateKeys), true);
    }

    public static Transaction beginTransaction() throws SQLException {
        return new Transaction(sConnPool.getConnection());
    }
}
