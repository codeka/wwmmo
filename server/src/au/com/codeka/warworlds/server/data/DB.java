package au.com.codeka.warworlds.server.data;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.postgresql.util.PSQLException;

import au.com.codeka.common.Log;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.IConnectionCustomizer;

/**
 * This is a wrapper class that helps us with connecting to the database.
 */
public class DB {
    private static final Log log = new Log("DB");
    private static String sDbName;
    private static String sUsername;
    private static String sPassword;
    private static String sSchema;
    private static Strategy sStrategy;

    static {
        try {
            sDbName = System.getProperty("au.com.codeka.warworlds.server.dbName");
            if (sDbName == null) {
                sDbName = "wwmmo";
            }

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
                log.error("Error loading PostgreSQL driver.", e);
                throw new RuntimeException("Error loading PostgreSQL driver.", e);
            }

            sStrategy = new HikariStrategy();
            //sStrategy = new NoConnectionPoolStrategy();
        } catch (Throwable e) {
            log.error("Some exception", e);
        }
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

    // PostgreSQL error codes are documented here:
    // http://www.postgresql.org/docs/current/static/errcodes-appendix.html

    /** Determines whether the given {@link SQLException} was a constraint violation. */
    public static boolean isConstraintViolation(SQLException e) {
        PSQLException psql = (PSQLException) e;
        return psql.getSQLState().startsWith("23");
    }

    /** Determines whether the given {@link SQLException} is retryable (e.g. deadlock, etc) */
    public static boolean isRetryable(SQLException e) {
        PSQLException psql = (PSQLException) e;
        return psql.getSQLState().startsWith("40");
    }

    private interface Strategy {
        SqlStmt prepare(String sql) throws SQLException;
        SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException;
        Transaction beginTransaction() throws SQLException;
    }

    //@SuppressWarnings("unused")
    private static class HikariStrategy implements Strategy {
        private final HikariDataSource dataSource;

        public HikariStrategy() {
            HikariConfig config = new HikariConfig();
            config.setMaximumPoolSize(40);
            config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
            config.setUsername(sUsername);
            config.setPassword(sPassword);
            config.addDataSourceProperty("serverName", "localhost");
            config.addDataSourceProperty("portNumber", "5432");
            config.addDataSourceProperty("databaseName", sDbName);
            config.setConnectionCustomizer(connectionCustomizer);
            dataSource = new HikariDataSource(config);
        }

        @Override
        public SqlStmt prepare(String sql) throws SQLException {
            Connection conn = dataSource.getConnection();
            return new SqlStmt(conn, sql, conn.prepareStatement(sql), true);
        }

        @Override
        public SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
            Connection conn = dataSource.getConnection();
            return new SqlStmt(conn, sql, conn.prepareStatement(sql, autoGenerateKeys), true);
        }

        @Override
        public Transaction beginTransaction() throws SQLException {
            return new Transaction(dataSource.getConnection());
        }

        private IConnectionCustomizer connectionCustomizer = new IConnectionCustomizer() {
            @Override
            public void customize(Connection connection) throws SQLException {
                try {
                    CallableStatement stmt = connection.prepareCall(
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
        private static String jdbcUrl;

        static {
            jdbcUrl = String.format("jdbc:postgresql://localhost:5432/%s", sDbName);
        }

        @Override
        public SqlStmt prepare(String sql) throws SQLException {
            Connection conn = DriverManager.getConnection(jdbcUrl, sUsername, sPassword);
            return new SqlStmt(conn, sql, conn.prepareStatement(sql), true);
        }

        @Override
        public SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
            Connection conn = DriverManager.getConnection(jdbcUrl, sUsername, sPassword);
            return new SqlStmt(conn, sql, conn.prepareStatement(sql, autoGenerateKeys), true);
        }

        @Override
        public Transaction beginTransaction() throws SQLException {
            return new Transaction(DriverManager.getConnection(jdbcUrl, sUsername, sPassword));
        }
    }
}
