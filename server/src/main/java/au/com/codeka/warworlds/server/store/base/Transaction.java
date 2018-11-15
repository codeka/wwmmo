package au.com.codeka.warworlds.server.store.base;

import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.PooledConnection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class for wrapping up a transaction.
 *
 * <p>We open our own connection to the data store, and use it to start a transaction.
 */
public class Transaction implements AutoCloseable {
  private final SQLiteConnectionPoolDataSource dataSource;
  private PooledConnection pooledConn;
  private Connection conn;
  private boolean pendingCommit;

  Transaction(SQLiteConnectionPoolDataSource dataSource) {
    this.dataSource = checkNotNull(dataSource);
  }

  Connection getConnection() throws SQLException {
    if (conn == null) {
      pooledConn = dataSource.getPooledConnection();
      conn = pooledConn.getConnection();
      conn.setAutoCommit(false);
      pendingCommit = true;
    }

    return conn;
  }

  public void commit() throws SQLException {
    pendingCommit = false;
    conn.commit();
  }

  public void abort() throws SQLException {
    pendingCommit = false;
    conn.rollback();
  }

  @Override
  public void close() throws Exception {
    if (pendingCommit) {
      abort();
    }
  }
}
