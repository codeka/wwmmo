package au.com.codeka.warworlds.server.store;

import java.util.ArrayList;
import java.util.Collection;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.proto.SuspiciousEvent;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;
import au.com.codeka.warworlds.server.store.base.StoreWriter;
import au.com.codeka.warworlds.server.store.base.Transaction;

import static au.com.codeka.warworlds.server.store.StatsHelper.timestampToDay;

/**
 * Store for keep track of suspicious events.
 */
public class SuspiciousEventStore extends BaseStore {
  private static final Log log = new Log("SuspiciousEventStore");

  protected SuspiciousEventStore(String fileName) {
    super(fileName);
  }

  public void add(Collection<SuspiciousEvent> events) {
    try (Transaction t = newTransaction()) {
      StoreWriter writer = newWriter(t)
          .stmt("INSERT INTO suspicious_events (" +
              "timestamp, day, empire_id, event) " +
              "VALUES (?, ?, ?, ?)");
      for (SuspiciousEvent event : events) {
          writer
              .param(0, event.timestamp)
              .param(1, timestampToDay(event.timestamp))
              .param(2, event.modification.empire_id)
              .param(3, event.encode())
              .execute();
      }
      t.commit();
    } catch (Exception e) {
      log.error("Unexpected", e);
    }
  }

  public Collection<SuspiciousEvent> query(/* TODO: search? */) {
    try (QueryResult res = newReader()
        .stmt("SELECT event FROM suspicious_events ORDER BY timestamp DESC")
        .query()) {
      ArrayList<SuspiciousEvent> events = new ArrayList<>();
      while (res.next()) {
        events.add(SuspiciousEvent.ADAPTER.decode(res.getBytes(0)));
      }
      return events;
    } catch (Exception e) {
      log.error("Unexpected.", e);
      return null;
    }
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
