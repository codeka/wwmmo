package au.com.codeka.warworlds.server.store.base;

import au.com.codeka.warworlds.server.store.StoreException;
import java.sql.SQLException;
import javax.annotation.Nullable;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

/** A helper class for reading from the data store. */
public class StoreReader extends StatementBuilder<StoreReader> {
  StoreReader(SQLiteConnectionPoolDataSource dataSource, @Nullable Transaction transaction) {
    super(dataSource, transaction);
  }

  public QueryResult query() throws StoreException {
    execute();
    try {
      return new QueryResult(transaction == null ? conn : null, stmt.getResultSet());
    } catch (SQLException e) {
      throw new StoreException(e);
    }
  }

  @Override
  public void close() {
    // The QueryResult should close us, nothing to do here.
  }
}
