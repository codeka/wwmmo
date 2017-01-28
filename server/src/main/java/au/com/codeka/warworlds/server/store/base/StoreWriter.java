package au.com.codeka.warworlds.server.store.base;


import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.sql.SQLException;

import au.com.codeka.warworlds.server.store.StoreException;

/** A helper class for writing to the data store. */
public class StoreWriter extends StatementBuilder<StoreWriter> {
  StoreWriter(SQLiteConnectionPoolDataSource dataSource) {
    super(dataSource);
  }

  @Override
  public void execute() throws StoreException {
    super.execute();

    try {
      pooledConnection.close();
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }
}
