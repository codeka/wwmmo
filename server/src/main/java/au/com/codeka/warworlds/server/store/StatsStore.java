package au.com.codeka.warworlds.server.store;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.DeviceInfo;
import au.com.codeka.warworlds.common.proto.LoginEvent;
import au.com.codeka.warworlds.common.proto.LoginRequest;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;

/**
 * Store for various stats that we want to keep track of.
 */
public class StatsStore extends BaseStore {
  private static final Log log = new Log("StatsStore");

  StatsStore(String fileName) {
    super(fileName);
  }

  /** Store the given {@link LoginRequest} */
  public void addLoginEvent(LoginRequest loginRequest, Account account) {
    long now = System.currentTimeMillis();
    try {
      newWriter()
          .stmt("INSERT INTO login_events (" +
              "timestamp, day, empire_id, device_id, email_addr, device_info) " +
              "VALUES (?, ?, ?, ?, ?, ?)")
          .param(0, now)
          .param(1, timestampToDay(now))
          .param(2, account.empire_id == null ? 0 : account.empire_id)
          .param(3, loginRequest.device_info.device_id)
          .param(4, account.email)
          .param(5, loginRequest.device_info.encode())
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected", e);
    }
  }

  /** Gets the most recent {@code num} login events. */
  public List<LoginEvent> getRecentLogins(int num) {
    ArrayList<LoginEvent> loginEvents = new ArrayList<>();
    try (
        QueryResult res = newReader()
            .stmt("SELECT timestamp, day, empire_id, email_addr, device_info FROM login_events ORDER BY timestamp DESC")
            .query()
        ) {
      while (res.next()) {
        loginEvents.add(new LoginEvent.Builder()
            .timestamp(res.getLong(0))
            .day(res.getInt(1))
            .empire_id(res.getLong(2))
            .email_addr(res.getString(3))
            .device_info(DeviceInfo.ADAPTER.decode(res.getBytes(4)))
            .build());
        if (loginEvents.size() >= num) {
          break;
        }
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return loginEvents;
  }

  /**
   * Take a stamp in unix-epoch-millis format (i.e. like what you'd get from
   * {@code System.currentTimeMillis()}, and return a "day" integer of the form yyyymmdd.
   */
  private int timestampToDay(long timestamp) {
    DateTime dt = new DateTime(timestamp);
    return dt.year().get() * 10000 + dt.monthOfYear().get() * 100 + dt.dayOfMonth().get();
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE login_events (" +
                  "  timestamp INTEGER," +
                  "  day INTEGER," +
                  "  empire_id INTEGER," +
                  "  device_id STRING," +
                  "  email_addr STRING," +
                  "  device_info BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_login_events_day ON login_events (day)")
          .execute();

      newWriter()
          .stmt(
              "CREATE TABLE create_empire_events (" +
                  "  timestamp INTEGER," +
                  "  day INTEGER," +
                  "  empire_id INTEGER)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_create_empire_events_day ON create_empire_events (day)")
          .execute();

      diskVersion++;
    }

    return diskVersion;
  }
}
