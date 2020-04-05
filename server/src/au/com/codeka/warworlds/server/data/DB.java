package au.com.codeka.warworlds.server.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.metrics.MetricsManager;

/**
 * This is a wrapper class that helps us with connecting to the database.
 */
public class DB {
  private static final Log log = new Log("DB");
  private static final HikariDataSource dataSource;
  private static final String schemaName;

  static {
    try {
      // Make sure the driver is loaded
      Class.forName("org.postgresql.Driver");

      Configuration.DatabaseConfiguration dbconfig = Configuration.i.getDatabaseConfig();
      schemaName = dbconfig.getSchema();

      HikariConfig config = new HikariConfig();
      config.setPoolName("db");
      config.setMaximumPoolSize(100);
      config.setMinimumIdle(8);
      config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
      config.setUsername(dbconfig.getUsername());
      config.setPassword(dbconfig.getPassword());
      config.addDataSourceProperty("serverName", dbconfig.getServer());
      config.addDataSourceProperty("portNumber", Integer.toString(dbconfig.getPort()));
      config.addDataSourceProperty("databaseName", dbconfig.getDatabase());
      config.setConnectionInitSql(String.format("SET search_path TO '%s'", schemaName));
      config.setMetricRegistry(MetricsManager.i.getMetricsRegistry());
      dataSource = new HikariDataSource(config);

      log.info("Database configured: username=%s, password=%s, schema=%s",
          dbconfig.getUsername(), dbconfig.getPassword(), dbconfig.getSchema());
    } catch (Exception e) {
      log.error("Error loading PostgreSQL driver.", e);
      throw new RuntimeException("Error loading PostgreSQL driver.", e);
    }
  }

  public static SqlStmt prepare(String sql) throws SQLException {
    Connection conn = dataSource.getConnection();
    return new SqlStmt(conn, sql, conn.prepareStatement(sql), true);
  }

  public static SqlStmt prepare(String sql, int autoGenerateKeys)
      throws SQLException {
    Connection conn = dataSource.getConnection();
    return new SqlStmt(conn, sql,
        conn.prepareStatement(sql, autoGenerateKeys), true);
  }

  public static Transaction beginTransaction() throws SQLException {
    return new Transaction(dataSource.getConnection());
  }
}
