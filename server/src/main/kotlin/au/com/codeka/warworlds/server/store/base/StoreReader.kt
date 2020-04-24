package au.com.codeka.warworlds.server.store.base

import au.com.codeka.warworlds.server.store.StoreException
import org.sqlite.javax.SQLiteConnectionPoolDataSource
import java.sql.SQLException

/** A helper class for reading from the data store.  */
class StoreReader
    internal constructor(dataSource: SQLiteConnectionPoolDataSource, transaction: Transaction?)
    : StatementBuilder<StoreReader>(dataSource, transaction) {

  fun query(): QueryResult {
    execute()
    return try {
      QueryResult(if (transaction == null) conn else null, stmt.resultSet)
    } catch (e: SQLException) {
      throw StoreException(e)
    }
  }

  override fun close() {
    // The QueryResult should close us, nothing to do here.
  }
}
