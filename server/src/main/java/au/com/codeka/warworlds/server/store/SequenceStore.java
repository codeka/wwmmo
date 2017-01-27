package au.com.codeka.warworlds.server.store;

/**
 * A store for storing a sequence of unique IDs. To keep this performant, we only query/update the
 * actual data store in batches. Each time we increment the counter in the data store by
 * {@link #DATA_STORE_INCREMENT_AMOUNT}, and then return values from that "pool" each time
 * {@link #nextIdentifier()} is called.
 */
public class SequenceStore extends BaseStore {
  private final Object lock = new Object();

  private final long DATA_STORE_INCREMENT_AMOUNT = 100L;

  /** The next identifier that we should return. */
  private long identifier;

  /**
   * The maximum identifier we can return without having to go back to the store to fetch another
   * batch.
   */
  private long maxIdentifier;

  SequenceStore(String fileName) {
    super(fileName);
  }

  /** Returns the next identifier in the sequence. */
  public long nextIdentifier() {
    synchronized (lock) {
      if (identifier == maxIdentifier) {
        try (QueryResult res = newReader().stmt("SELECT id FROM identifiers").query()) {
          if (!res.next()) {
            throw new RuntimeException("Expected at least one row in identifiers table.");
          }
          identifier = res.getLong(0);
          maxIdentifier = identifier + DATA_STORE_INCREMENT_AMOUNT;
        } catch (Exception e) {
          // We can't continue if this fails, it'll cause irreversible corruption.
          throw new RuntimeException(e);
        }

        try {
          newWriter()
              .stmt("UPDATE identifiers SET id = ?")
              .param(0, maxIdentifier)
              .execute();
        } catch (StoreException e) {
          // We can't continue if this fails, it'll cause irreversible corruption.
          throw new RuntimeException(e);
        }
      }

      identifier++;
      return identifier;
    }
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt("CREATE TABLE identifiers (id INTEGER PRIMARY KEY)")
          .execute();
      newWriter()
          .stmt("INSERT INTO identifiers (id) VALUES (100)")
          .execute();
      diskVersion ++;
    }

    return diskVersion;
  }
}
