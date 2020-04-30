package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.server.store.base.BaseStore

/**
 * A store for storing a sequence of unique IDs. To keep this performant, we only query/update the
 * actual data store in batches. Each time we increment the counter in the data store by
 * [.DATA_STORE_INCREMENT_AMOUNT], and then return values from that "pool" each time
 * [.nextIdentifier] is called.
 */
class SequenceStore internal constructor(fileName: String) : BaseStore(fileName) {
  private val lock = Any()
  private val DATA_STORE_INCREMENT_AMOUNT = 100L

  /** The next identifier that we should return.  */
  private var identifier: Long = 0

  /**
   * The maximum identifier we can return without having to go back to the store to fetch another
   * batch.
   */
  private var maxIdentifier: Long = 0

  /** Returns the next identifier in the sequence.  */
  fun nextIdentifier(): Long {
    synchronized(lock) {
      if (identifier == maxIdentifier) {
        try {
          newReader().stmt("SELECT id FROM identifiers").query().use { res ->
            if (!res.next()) {
              throw RuntimeException("Expected at least one row in identifiers table.")
            }
            identifier = res.getLong(0)
            maxIdentifier = identifier + DATA_STORE_INCREMENT_AMOUNT
          }
        } catch (e: Exception) {
          // We can't continue if this fails, it'll cause irreversible corruption.
          throw RuntimeException(e)
        }
        try {
          newWriter()
              .stmt("UPDATE identifiers SET id = ?")
              .param(0, maxIdentifier)
              .execute()
        } catch (e: StoreException) {
          // We can't continue if this fails, it'll cause irreversible corruption.
          throw RuntimeException(e)
        }
      }
      identifier++
      return identifier
    }
  }

  @Throws(StoreException::class)
  override fun onOpen(diskVersion: Int): Int {
    var diskVersion = diskVersion
    if (diskVersion == 0) {
      newWriter()
          .stmt("CREATE TABLE identifiers (id INTEGER PRIMARY KEY)")
          .execute()
      newWriter()
          .stmt("INSERT INTO identifiers (id) VALUES (100)")
          .execute()
      diskVersion++
    }
    return diskVersion
  }
}