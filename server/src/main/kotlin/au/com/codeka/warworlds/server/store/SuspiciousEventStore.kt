package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.server.proto.SuspiciousEvent
import au.com.codeka.warworlds.server.store.base.BaseStore
import java.util.*

/**
 * Store for keep track of suspicious events.
 */
class SuspiciousEventStore(fileName: String) : BaseStore(fileName) {
  fun add(events: Collection<SuspiciousEvent>) {
    newTransaction().use { t ->
      val writer = newWriter(t)
          .stmt("INSERT INTO suspicious_events (" +
              "timestamp, day, empire_id, event) " +
              "VALUES (?, ?, ?, ?)")
      for (event in events) {
        writer
            .param(0, event.timestamp)
            .param(1, StatsHelper.timestampToDay(event.timestamp))
            .param(2, event.modification.empire_id)
            .param(3, event.encode())
            .execute()
      }
      t.commit()
    }
  }

  fun query( /* TODO: search? */): Collection<SuspiciousEvent> {
    newReader()
        .stmt("SELECT event FROM suspicious_events ORDER BY timestamp DESC")
        .query().use { res ->
          val events: ArrayList<SuspiciousEvent> = ArrayList<SuspiciousEvent>()
          while (res.next()) {
            events.add(SuspiciousEvent.ADAPTER.decode(res.getBytes(0)))
          }
          return events
        }
  }

  override fun onOpen(diskVersion: Int): Int {
    var version = diskVersion
    if (version == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE suspicious_events (" +
                  "  timestamp INTEGER," +
                  "  day INTEGER," +
                  "  empire_id INTEGER," +
                  "  event BLOB)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_suspicious_events_day ON suspicious_events (day)")
          .execute()
      version++
    }
    return version
  }
}