package au.com.codeka.warworlds.server.data;

import java.sql.Connection;
import java.sql.SQLException;

import au.com.codeka.common.Log;

public class Transaction implements AutoCloseable {
  private static final Log log = new Log("Transaction");

  private Connection conn;
  private boolean wasCommitted;

  public Transaction(Connection conn) throws SQLException {
    this.conn = conn;
    this.conn.setAutoCommit(false);
    log.info("Starting transaction with isolation level: %d", this.conn.getTransactionIsolation());
  }

  public SqlStmt prepare(String sql) throws SQLException {
    return new SqlStmt(conn, sql, conn.prepareStatement(sql), false);
  }

  public SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
    return new SqlStmt(conn, sql, conn.prepareStatement(sql, autoGenerateKeys), false);
  }

  public void commit() throws SQLException {
    conn.commit();
    wasCommitted = true;
  }

  public void rollback() throws SQLException {
    conn.rollback();
    wasCommitted = true;
  }

  @Override
  public void close() throws Exception {
    if (!wasCommitted) {
      conn.rollback();
    }
    conn.setAutoCommit(true);
    conn.close();
  }
}
