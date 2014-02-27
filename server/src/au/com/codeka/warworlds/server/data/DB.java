package au.com.codeka.warworlds.server.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

/**
 * This is a wrapper class that helps us with connecting to the database.
 */
public class DB {
    private static final Logger log = LoggerFactory.getLogger(DB.class);
    private static String sJdbcUrl;
    private static String sUsername;
    private static String sPassword;
    private static Strategy sStrategy;

    static {
        String dbName = System.getProperty("au.com.codeka.warworlds.server.dbName");
        if (dbName == null) {
            dbName = "wwmmo";
        }
        sJdbcUrl = String.format("jdbc:mysql://localhost:3306/%s?useUnicode=true&characterEncoding=utf8", dbName);

        sUsername = System.getProperty("au.com.codeka.warworlds.server.dbUser");
        if (sUsername == null) {
            sUsername = "wwmmo_user";
        }

        sPassword = System.getProperty("au.com.codeka.warworlds.server.dbPass");
        if (sPassword == null) {
            sPassword = "H98765gf!s876#Hdf2%7f";
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // TODO: should never happen!
        }

        sStrategy = new BoneCPStrategy();
        //sStrategy = new NoConnectionPoolStrategy();
    }

    public static SqlStmt prepare(String sql) throws SQLException {
        return sStrategy.prepare(sql);
    }

    public static SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
        return sStrategy.prepare(sql, autoGenerateKeys);
    }

    public static Transaction beginTransaction() throws SQLException {
        return sStrategy.beginTransaction();
    }

    private interface Strategy {
        SqlStmt prepare(String sql) throws SQLException;
        SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException;
        Transaction beginTransaction() throws SQLException;
    }

    //@SuppressWarnings("unused")
    private static class BoneCPStrategy implements Strategy {
        private BoneCP mConnPool;

        public BoneCPStrategy() {
            BoneCPConfig config = new BoneCPConfig();
            config.setJdbcUrl(sJdbcUrl);
            config.setUsername(sUsername);
            config.setPassword(sPassword);
            config.setPartitionCount(4);
            config.setMaxConnectionsPerPartition(20);
            config.setConnectionTimeoutInMs(10000);
            config.setReleaseHelperThreads(0);
            config.setStatementReleaseHelperThreads(0);
            try {
                mConnPool = new BoneCP(config);
            } catch (SQLException e) {
                log.error("Could not create connection pool!", e);
            }
        }

        @Override
        public SqlStmt prepare(String sql) throws SQLException {
            Connection conn = mConnPool.getConnection();
            return new SqlStmt(conn, sql, conn.prepareStatement(sql), true);
        }

        @Override
        public SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
            Connection conn = mConnPool.getConnection();
            return new SqlStmt(conn, sql, conn.prepareStatement(sql, autoGenerateKeys), true);
        }

        @Override
        public Transaction beginTransaction() throws SQLException {
            return new Transaction(mConnPool.getConnection());
        }
    }

    @SuppressWarnings("unused") // we're using BoneCP now
    private static class NoConnectionPoolStrategy implements Strategy {
        @Override
        public SqlStmt prepare(String sql) throws SQLException {
            Connection conn = DriverManager.getConnection(sJdbcUrl, sUsername, sPassword);
            return new SqlStmt(conn, sql, conn.prepareStatement(sql), true);
        }

        @Override
        public SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
            Connection conn = DriverManager.getConnection(sJdbcUrl, sUsername, sPassword);
            return new SqlStmt(conn, sql, conn.prepareStatement(sql, autoGenerateKeys), true);
        }

        @Override
        public Transaction beginTransaction() throws SQLException {
            return new Transaction(DriverManager.getConnection(sJdbcUrl, sUsername, sPassword));
        }
    }
}
