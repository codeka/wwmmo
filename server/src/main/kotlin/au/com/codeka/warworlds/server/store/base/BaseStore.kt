package au.com.codeka.warworlds.server.store.base

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.server.store.StoreException
import com.google.common.base.Preconditions
import org.sqlite.SQLiteConfig
import org.sqlite.javax.SQLiteConnectionPoolDataSource
import java.sql.SQLException
import java.util.*
import javax.sql.PooledConnection

/**
 * Base storage object for interfacing with the data store.
 *
 *
 * Each [BaseStore] represents a single on-disk file, opening the store will load the
 * database, ensure that it's the correct version and upgrade the version if it's not. You can then
 * use [.newReader] and [.newWriter] for creating readers/writers into the data store.
 */
abstract class BaseStore protected constructor(private val fileName: String) {
  private lateinit var dataSource: SQLiteConnectionPoolDataSource

  fun open() {
    try {
      val config = SQLiteConfig()

      // Disable fsync calls, trusting that the filesystem will do the right thing. It's not always
      // the best assumption, but we are fine with losing ~1 day of data (basically, the time
      // between backups). Additionally, switch to write-ahead-logging for the journal. We could
      // turn it off completely as well, and rely on backups in the event of data loss, but that's
      // slightly more painful for development (where "crashes" are more likely).
      config.setSynchronous(SQLiteConfig.SynchronousMode.OFF)
      config.setJournalMode(SQLiteConfig.JournalMode.WAL)

      // Increase the cache size, we have plenty of memory on the server.
      config.setCacheSize(2048)

      // Enforce foreign key constraints (for some reason, the default is off)
      config.enforceForeignKeys(true)
      dataSource = SQLiteConnectionPoolDataSource(config)
      dataSource.pooledConnection
      dataSource.url = "jdbc:sqlite:data/store/$fileName"
    } catch (e: SQLException) {
      throw StoreException(e)
    }
    ensureVersion()
  }

  fun close() {
    // TODO: close? dataSource
  }

  protected fun newTransaction(): Transaction {
    return Transaction(dataSource)
  }

  protected fun newReader(): StoreReader {
    return StoreReader(dataSource, null)
  }

  protected fun newReader(transaction: Transaction?): StoreReader {
    return StoreReader(dataSource, transaction)
  }

  protected fun newWriter(): StoreWriter {
    return StoreWriter(dataSource, null)
  }

  protected fun newWriter(transaction: Transaction?): StoreWriter {
    return StoreWriter(dataSource, transaction)
  }

  /**
   * Called when the database is opened. Sub classes should implement this to perform any on-disk
   * upgrades required to get the database up to the current version.
   *
   * @param diskVersion The version of the database on-disk. A value of 0 indicates that the data
   * is brand-new on disk and all data structures should be created.
   * @return The new version to return as the on-disk version. Must be > 0 and >= the value passed
   * in as diskVersion.
   */
  protected abstract fun onOpen(diskVersion: Int): Int

  /** Check that the version of the database on disk is the same as the version we expect.  */
  private fun ensureVersion() {
    var currVersion = 0
    val conn = try {
      dataSource.pooledConnection
    } catch (e: SQLException) {
      return
    }
    try {
      conn.connection.createStatement().use { stmt ->
        val sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='version'"
        stmt.executeQuery(sql).use { rs ->
          if (rs.next()) {
            stmt.executeQuery("SELECT val FROM version").use { rs2 ->
              if (rs2.next()) {
                currVersion = rs2.getInt(1)
              }
            }
          }
        }
      }
    } catch (e: SQLException) {
      throw StoreException(e)
    }
    log.debug("%s version: %d", fileName, currVersion)
    val newVersion = onOpen(currVersion)
    if (newVersion == 0 || newVersion < currVersion) {
      throw StoreException(String.format(Locale.US,
          "Version returned from onOpen must be > 0 and >= the previous value. newVersion=%d, " +
              "currVersion=%d",
          newVersion, currVersion))
    }

    // Store the new version on disk as well
    if (newVersion != currVersion) {
      try {
        conn.connection.createStatement().use { stmt ->
          if (currVersion == 0) {
            stmt.executeUpdate("CREATE TABLE version (val INTEGER)")
            stmt.executeUpdate("INSERT INTO version (val) VALUES (0)")
          }
          stmt.executeUpdate(String.format(Locale.US, "UPDATE version SET val=%d", newVersion))
          log.debug("%s new version: %d", fileName, newVersion)
        }
      } catch (e: SQLException) {
        throw StoreException(e)
      }
    }
  }

  companion object {
    private val log = Log("BaseStore")
  }

}