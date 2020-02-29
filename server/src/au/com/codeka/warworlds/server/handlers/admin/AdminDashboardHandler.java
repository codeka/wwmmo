package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.Days;

import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.monitor.RequestStatMonitor;

import com.google.gson.JsonObject;

public class AdminDashboardHandler extends AdminHandler {
  private static final Log log = new Log("AdminDashboardHandler");

  @Override
  protected void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }

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
    data.put("active_empires_graph", graphData);

    ArrayList<Messages.RequestStatHour> stats = RequestStatMonitor.i.getLastHours(48);

    // Figure out for each empire in the set the number of requests in total they've sent over
    // the time period.
    HashSet<Integer> allEmpires = new HashSet<>();
    HashMap<Integer, Integer> topRequestingEmpires = new HashMap<>();
    int numStats = 10;
    for (Messages.RequestStatHour stat : stats) {
      for (Messages.RequestStatEmpireHour empireStat : stat.getEmpireList()) {
        allEmpires.add(empireStat.getEmpireId());
        Integer n = topRequestingEmpires.get(empireStat.getEmpireId());
        if (n == null) {
          n = 0;
        }
        n = n + empireStat.getTotalRequests();
        topRequestingEmpires.put(empireStat.getEmpireId(), n);
      }
      numStats --;
      if (numStats <= 0) {
        break;
      }
    }

    // We just want the top 10 empires, by number of requests
    int TOP = 10;
    PriorityQueue<Integer> empiresPriorityQueue =
        new PriorityQueue<>(
            (lhs, rhs) -> topRequestingEmpires.get(rhs) - topRequestingEmpires.get(lhs));
    empiresPriorityQueue.addAll(allEmpires);
    HashMap<Integer, Empire> topEmpires = new HashMap<>();
    for (Integer empireId : empiresPriorityQueue) {
      Empire empire = new EmpireController().getEmpire(empireId);
      topEmpires.put(empireId, empire);
      if (topEmpires.size() >= TOP) {
        break;
      }
    }

    ArrayList<HashMap<Object, Object>> requestGraphData = new ArrayList<>();
    for (int i = 0; i < stats.size(); i++) {
      Messages.RequestStatHour stat = stats.get(i);
      HashMap<Object, Object> graphEntry = new HashMap<>();
      int year = stat.getDay() / 10000;
      int month = (stat.getDay() - (year * 10000)) / 100;
      int day = stat.getDay() - (year * 10000) - (month * 100);
      int hour = stat.getHour();

      graphEntry.put("date", new DateTime(year, month + 1, day, hour, 0, 0));
      graphEntry.put("total", stat.getTotalRequests());
      for (Messages.RequestStatEmpireHour empireHour : stat.getEmpireList()) {
        Empire empire = topEmpires.get(empireHour.getEmpireId());
        if (empire != null) {
          graphEntry.put(empire.getID(), empireHour.getTotalRequests());
        }
      }
      requestGraphData.add(graphEntry);
    }
    data.put("request_graph", requestGraphData);
    data.put("top_request_empires", topEmpires);

    render("admin/index.html", data);
  }

  @Override
  protected void post() throws RequestException {
    if (!isAdmin()) {
      return;
    }

    JsonObject json = new JsonObject();
    populateOldestStar(json);
    setResponseJson(json);
  }

  private void populateOldestStar(JsonObject json) {
    DateTime lastSimulation = null;
    String sql = "SELECT stars.id, stars.name, stars.last_simulation FROM stars"
        + " WHERE empire_count > 0"
        + " ORDER BY last_simulation ASC LIMIT 1";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        lastSimulation = res.getDateTime(3);
        TimeFormatter fmt = TimeFormatter.create().withMaxDays(1000).withAlwaysIncludeMinutes(true);
        json.addProperty("oldest_star_id", res.getInt(1));
        json.addProperty("oldest_star_name", res.getString(2));
        json.addProperty("oldest_star_time", fmt.format(res.getDateTime(3)));
      }
    } catch (Exception e) {
      log.error("Error fetching oldest star.", e);
    }

    if (lastSimulation != null) {
      sql = "SELECT COUNT(*) FROM stars"
          + " WHERE empire_count > 0 AND last_simulation < ?";
      try (SqlStmt stmt = DB.prepare(sql)) {
        stmt.setDateTime(1, DateTime.now().minusHours(3));
        SqlResult res = stmt.select();
        while (res.next()) {
          json.addProperty("num_stars_older_than_3_hrs", res.getInt(1));
        }
      } catch (Exception e) {
        log.error("Error fetching number of stars to simulate.", e);
      }
    }
  }
}
