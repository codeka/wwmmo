package au.com.codeka.warworlds.server.store.base;


import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.PooledConnection;

import au.com.codeka.warworlds.server.store.StoreException;

public class QueryResult implements AutoCloseable {
  private final PooledConnection pooledConn;
  private final ResultSet rs;

  QueryResult(PooledConnection pooledConn, ResultSet rs) {
    this.pooledConn = pooledConn;
    this.rs = rs;
  }

  public boolean next() throws StoreException {
    try {
      return rs.next();
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
    pooledConn.close();
  }
}
