package au.com.codeka.warworlds.server.store.base;

import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.server.store.StoreException;

/** A helper class for writing to the data store. */
public class StoreWriter extends StatementBuilder<StoreWriter> {
  StoreWriter(SQLiteConnectionPoolDataSource dataSource, @Nullable Transaction transaction) {
    super(dataSource, transaction);
  }

  @Override
  public void execute() throws StoreException {
    super.execute();
    if (transaction == null) {
      close();
    }
  }
}
