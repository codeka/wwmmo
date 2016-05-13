package au.com.codeka.warworlds.server.admin.handlers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.Days;

import com.google.gson.JsonObject;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.admin.RequestException;

public class DashboardHandler extends AdminHandler {
  private static final Log log = new Log("DashboardHandler");

  @Override
  protected void get() throws RequestException {
    TreeMap<String, Object> data = new TreeMap<>();

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
