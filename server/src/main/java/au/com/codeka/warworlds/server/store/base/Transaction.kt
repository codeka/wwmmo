package au.com.codeka.warworlds.server.store.base

import com.google.common.base.Preconditions
import org.sqlite.javax.SQLiteConnectionPoolDataSource
import java.sql.Connection
import java.sql.SQLException
import javax.sql.PooledConnection

/**
 * Helper class for wrapping up a transaction.
 *
 *
 * We open our own connection to the data store, and use it to start a transaction.
 */
class Transaction internal constructor(dataSource: SQLiteConnectionPoolDataSource) : AutoCloseable {
  private var pooledConn: PooledConnection = dataSource.pooledConnection
  private var pendingCommit = false

  val conn: Connection = pooledConn.connection

  init {
    conn.autoCommit = false
    pendingCommit = true
  }

  fun commit() {
    pendingCommit = false
    conn.commit()
  }

  fun abort() {
    pendingCommit = false
    conn.rollback()
  }

  override fun close() {
    if (pendingCommit) {
      abort()
    }
  }

}