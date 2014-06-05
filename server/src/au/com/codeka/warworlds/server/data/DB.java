package au.com.codeka.warworlds.server.data;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import com.jolbox.bonecp.hooks.ConnectionHook;

/**
 * This is a wrapper class that helps us with connecting to the database.
 */
public class DB {
    private static final Logger log = LoggerFactory.getLogger(DB.class);
    private static String sJdbcUrl;
    private static String sUsername;
    private static String sPassword;
    private static String sSchema;
    private static Strategy sStrategy;

    static {
        String dbName = System.getProperty("au.com.codeka.warworlds.server.dbName");
        if (dbName == null) {
            dbName = "wwmmo";
        }
        sJdbcUrl = String.format("jdbc:postgresql://localhost:5432/%s", dbName);

        sSchema = System.getProperty("au.com.codeka.warworlds.server.dbSchema");
        if (sSchema == null) {
            sSchema = "beta";
        }

        sUsername = System.getProperty("au.com.codeka.warworlds.server.dbUser");
        if (sUsername == null) {
            sUsername = "wwmmo_user";
        }

        sPassword = System.getProperty("au.com.codeka.warworlds.server.dbPass");
        if (sPassword == null) {
            sPassword = "H98765gf!s876#Hdf2%7f";
        }

        try {
            Class.forName("org.postgresql.Driver");
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

    /** Determines whether the given {@link SQLException} was a constraint violation. */
    public static boolean isConstraintViolation(SQLException e) {
        return false; // TODO
    }

    /** Determines whether the given {@link SQLException} is retryable (e.g. deadlock, etc) */
    public static boolean isRetryable(SQLException e) {
        return false; // TODO
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
            config.setMaxConnectionsPerPartition(50);
            config.setConnectionTimeoutInMs(10000);
            config.setReleaseHelperThreads(0);
            config.setStatementReleaseHelperThreads(0);
            config.setConnectionHook(mConnectionHook);
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

        private ConnectionHook mConnectionHook = new AbstractConnectionHook() {
            @Override
            public void onAcquire(ConnectionHandle connection) {
                try {
                    CallableStatement stmt = connection.getInternalConnection().prepareCall(
                            String.format("SET search_path TO '%s'", sSchema));
                    stmt.execute();
                } catch (SQLException e) {
                    log.error("Exception caught trying to set schema.", e);
                }
            }
        };
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
