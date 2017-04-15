package au.com.codeka.warworlds.server.admin.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.LoginEvent;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

public class DashboardHandler extends AdminHandler {
  private static final Log log = new Log("DashboardHandler");

  /** Any role can visit this page. */
  @Override
  protected Collection<AdminRole> getRequiredRoles() {
    return Arrays.asList(AdminRole.values());
  }

  @Override
  protected void get() throws RequestException {
    TreeMap<String, Object> data = new TreeMap<>();

    List<LoginEvent> loginEvents = DataStore.i.stats().getRecentLogins(10);
    data.put("loginEvents", loginEvents);

    HashMap<Long, Empire> empires = new HashMap<>();
    for (LoginEvent loginEvent : loginEvents) {
      if (!empires.containsKey(loginEvent.empire_id)) {
        empires.put(loginEvent.empire_id, DataStore.i.empires().get(loginEvent.empire_id));
      }
    }
    data.put("empires", empires);

    DateTime now = DateTime.now();
    ArrayList<TreeMap<String, Object>> graphData = new ArrayList<>();
    for (int i = 0; i < 60; i++) {
      TreeMap<String, Object> graphEntry = new TreeMap<>();
      DateTime dt = now.minusDays(i);
      Calendar c = Calendar.getInstance();
      c.setTime(dt.toDate());
      graphEntry.put("year", c.get(Calendar.YEAR));
      graphEntry.put("month", c.get(Calendar.MONTH) - 1);
      graphEntry.put("day", c.get(Calendar.DAY_OF_MONTH));
      graphData.add(graphEntry);
    }
/*
    String sql = "SELECT date, active_1d, active_7d, new_signups" + " FROM dashboard_stats"
        + " ORDER BY date DESC" + " LIMIT 60";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        DateTime dt = res.getDateTime(1);
        int oneDA = res.getInt(2);
        int sevenDA = res.getInt(3);
        int newSignups = res.getInt(4);

        int index = Days.daysBetween(dt, now).getDays();
        if (index < 60) {
          TreeMap<String, Object> graphEntry = graphData.get(index);
          graphEntry.put("oneda", oneDA);
          graphEntry.put("sevenda", sevenDA);
          graphEntry.put("signups", newSignups);
        }
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }
    data.put("graph_data", graphData);
*/
    render("index.html", data);
  }
}
