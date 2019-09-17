package au.com.codeka.warworlds.server.data;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.IConnectionCustomizer;

/**
 * This is a wrapper class that helps us with connecting to the database.
 */
public class DB {
  private static final Log log = new Log("DB");
  private static final HikariDataSource dataSource;
  private static final String schemaName;

  private static final IConnectionCustomizer connectionCustomizer = new IConnectionCustomizer() {
    @Override
    public void customize(Connection connection) {
      try {
        CallableStatement stmt = connection.prepareCall(
            String.format("SET search_path TO '%s'", schemaName));
        stmt.execute();
        log.debug("New connection created in schema: %s", schemaName);
      } catch (SQLException e) {
        log.error("Exception caught trying to set schema.", e);
      }
    }
  };

  static {
    try {
      // Make sure the driver is loaded
      Class.forName("org.postgresql.Driver");

      Configuration.DatabaseConfiguration dbconfig = Configuration.i.getDatabaseConfig();
      HikariConfig config = new HikariConfig();
      config.setMaximumPoolSize(40);
      config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
      config.setUsername(dbconfig.getUsername());
      config.setPassword(dbconfig.getPassword());
      config.addDataSourceProperty("serverName", dbconfig.getServer());
      config.addDataSourceProperty("portNumber", Integer.toString(dbconfig.getPort()));
      config.addDataSourceProperty("databaseName", dbconfig.getDatabase());
      config.setConnectionCustomizer(connectionCustomizer);
      dataSource = new HikariDataSource(config);
      schemaName = dbconfig.getSchema();

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
