package au.com.codeka.warworlds.server.store;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.DeviceInfo;
import au.com.codeka.warworlds.common.proto.LoginRequest;
import au.com.codeka.warworlds.server.proto.DailyStat;
import au.com.codeka.warworlds.server.proto.LoginEvent;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;

import static au.com.codeka.warworlds.server.store.StatsHelper.dateTimeToDay;
import static au.com.codeka.warworlds.server.store.StatsHelper.timestampToDay;

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

  /** Get the {@link DailyStat} for the last {@code num} days. */
  public Map<Integer, DailyStat> getDailyStats(int num) {
    DateTime dt = DateTime.now().minusDays(num + 7); // 7 more to calculate the 7da correctly
    int currDay = dateTimeToDay(dt);

    ArrayList<Set<Long>> lastEmpires = new ArrayList<>();
    lastEmpires.add(new HashSet<>());

    HashMap<Integer, DailyStat> dailyStats = new HashMap<>();
    try (
        QueryResult res = newReader()
            .stmt("SELECT day, empire_id FROM login_events WHERE day >= ? ORDER BY day ASC")
            .param(0, currDay)
            .query()
    ) {
      while (res.next()) {
        int day = res.getInt(0);
        long empireId = res.getLong(1);
        if (day == currDay) {
          lastEmpires.get(0).add(empireId);
        } else {
          appendStats(dailyStats, currDay, lastEmpires);
          currDay = day;
          lastEmpires.add(0, new HashSet<>());
          lastEmpires.get(0).add(empireId);
        }
      }
      appendStats(dailyStats, currDay, lastEmpires);
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return dailyStats;
  }

  private void appendStats(
      Map<Integer, DailyStat> dailyStats,
      int day,
      ArrayList<Set<Long>> lastEmpires) {
    Set<Long> sevenda = new HashSet<>();
    for (int i = 0; i < 7 && i < lastEmpires.size(); i++) {
      sevenda.addAll(lastEmpires.get(i));
    }

    dailyStats.put(day,
        new DailyStat.Builder()
            .day(day)
            .oneda(lastEmpires.get(0).size())
            .sevenda(sevenda.size())
            .signups(0) // TODO: populate
            .build());
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
