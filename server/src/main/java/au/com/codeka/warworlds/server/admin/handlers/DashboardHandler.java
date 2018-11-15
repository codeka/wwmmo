package au.com.codeka.warworlds.server.admin.handlers;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.proto.DailyStat;
import au.com.codeka.warworlds.server.proto.LoginEvent;
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

    DateTime dt = DateTime.now().minusDays(60);
    Map<Integer, DailyStat> dailyStats = DataStore.i.stats().getDailyStats(60);
    List<DailyStat> graph = new ArrayList<>();
    for (int i = 0; i <= 60; i++) {
      int day = dt.year().get() * 10000 + dt.monthOfYear().get() * 100 + dt.dayOfMonth().get();
      DailyStat stat = dailyStats.get(day);
      if (stat == null) {
        stat = new DailyStat.Builder()
            .day(day)
            .oneda(0)
            .sevenda(0)
            .signups(0)
            .build();
      }
      graph.add(stat);
      dt = dt.plusDays(1);
    }
    data.put("graph", graph);

    render("index.html", data);
  }
}
