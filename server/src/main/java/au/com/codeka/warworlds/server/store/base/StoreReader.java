package au.com.codeka.warworlds.server.store.base;

import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.sql.SQLException;

import au.com.codeka.warworlds.server.store.StoreException;

/** A helper class for reading from the data store. */
public class StoreReader extends StatementBuilder<StoreReader> {
  StoreReader(SQLiteConnectionPoolDataSource dataSource) {
    super(dataSource);
  }

  public QueryResult query() throws StoreException {
    execute();
    try {
      return new QueryResult(pooledConnection, stmt.getResultSet());
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }
}
