package au.com.codeka.warworlds.server.store;

import au.com.codeka.warworlds.server.store.base.BaseStore;

/**
 * Store for keep track of suspicious events.
 */
public class SuspiciousEventStore extends BaseStore {
  protected SuspiciousEventStore(String fileName) {
    super(fileName);
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE suspicious_events (" +
                  "  timestamp INTEGER," +
                  "  day INTEGER," +
                  "  empire_id INTEGER," +
                  "  event BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_suspicious_events_day ON suspicious_events (day)")
          .execute();

      diskVersion++;
    }

    return diskVersion;
  }
}
