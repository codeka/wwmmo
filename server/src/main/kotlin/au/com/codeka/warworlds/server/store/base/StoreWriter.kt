package au.com.codeka.warworlds.server.store.base

import org.sqlite.javax.SQLiteConnectionPoolDataSource

/** A helper class for writing to the data store.  */
class StoreWriter
  internal constructor(dataSource: SQLiteConnectionPoolDataSource, transaction: Transaction?)
  : StatementBuilder<StoreWriter>(dataSource, transaction) {

  override fun execute(): Int {
    val count = super.execute()
    if (transaction == null) {
      close()
    }
    return count
  }
}
