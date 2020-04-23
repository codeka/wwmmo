package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.common.Log
import java.io.File

/** Wraps our references to the various data store objects.  */
class DataStore private constructor() {
  private val accounts = AccountsStore("accounts.db")
  private val adminUsers = AdminUsersStore("admin.db")
  private val chat = ChatStore("chat.db")
  private val empires = EmpiresStore("empires.db")
  private val seq = SequenceStore("seq.db")
  private val sectors = SectorsStore("sectors.db")
  private val stars = StarsStore("stars.db")
  private val stats = StatsStore("stats.db")
  private val suspiciousEvents = SuspiciousEventStore("suss-events.db")
  fun open() {
    // Make sure we open the sqlite JDBC driver.
    try {
      Class.forName("org.sqlite.JDBC")
    } catch (e: ClassNotFoundException) {
      throw RuntimeException("Error loading SQLITE JDBC driver.", e)
    }
    val home = File("data/store")
    if (!home.exists()) {
      if (!home.mkdirs()) {
        throw RuntimeException("Error creating directories for data store.")
      }
    }
    try {
      accounts.open()
      adminUsers.open()
      chat.open()
      empires.open()
      seq.open()
      sectors.open()
      stars.open()
      stats.open()
      suspiciousEvents.open()
    } catch (e: StoreException) {
      log.error("Error creating databases.", e)
      throw RuntimeException(e)
    }
  }

  fun close() {
    try {
      log.info("Closing data tables...")
      suspiciousEvents.close()
      stats.close()
      stars.close()
      sectors.close()
      seq.close()
      empires.close()
      chat.close()
      adminUsers.close()
      accounts.close()
    } catch (e: StoreException) {
      log.error("Error closing databases.", e)
    }
  }

  fun adminUsers(): AdminUsersStore {
    return adminUsers
  }

  fun accounts(): AccountsStore {
    return accounts
  }

  fun chat(): ChatStore {
    return chat
  }

  fun empires(): EmpiresStore {
    return empires
  }

  fun seq(): SequenceStore {
    return seq
  }

  fun sectors(): SectorsStore {
    return sectors
  }

  fun stars(): StarsStore {
    return stars
  }

  fun stats(): StatsStore {
    return stats
  }

  fun suspiciousEvents(): SuspiciousEventStore {
    return suspiciousEvents
  }

  companion object {
    private val log = Log("DataStore")
    val i = DataStore()
  }
}