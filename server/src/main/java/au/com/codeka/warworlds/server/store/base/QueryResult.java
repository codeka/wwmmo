package au.com.codeka.warworlds.server.store.base;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.server.store.StoreException;

public class QueryResult implements AutoCloseable {
  @Nullable private final Connection conn;
  private final ResultSet rs;

  QueryResult(@Nullable Connection conn, ResultSet rs) {
    this.conn = conn;
    this.rs = rs;
  }

  public boolean next() throws StoreException {
    try {
      return rs.next();
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }

  public String getString(int columnIndex) throws StoreException {
    try {
      return rs.getString(columnIndex + 1);
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }

  public int getInt(int columnIndex) throws StoreException {
    try {
      return rs.getInt(columnIndex + 1);
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }

  public long getLong(int columnIndex) throws StoreException {
    try {
      return rs.getLong(columnIndex + 1);
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }

  public byte[] getBytes(int columnIndex) throws StoreException {
    try {
      return rs.getBytes(columnIndex + 1);
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }

  @Override
  public void close() throws Exception {
    rs.close();
    if (conn != null) {
      conn.close();
    }
  }
}
