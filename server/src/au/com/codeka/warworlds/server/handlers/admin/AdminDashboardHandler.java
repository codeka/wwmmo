package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.Days;

import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

import com.google.gson.JsonObject;

public class AdminDashboardHandler extends AdminHandler {
  private static final Log log = new Log("AdminDashboardHandler");

  @Override
  protected void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }

    TreeMap<String, Object> data = new TreeMap<String, Object>();

    DateTime now = DateTime.now();
    ArrayList<TreeMap<String, Object>> graphData = new ArrayList<TreeMap<String, Object>>();
    for (int i = 0; i < 60; i++) {
      TreeMap<String, Object> graphEntry = new TreeMap<String, Object>();
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
    data.put("graph_data", graphData);

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
        + " ORDER BY last_simulation ASC LIMIT 2";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        lastSimulation = res.getDateTime(3);
        json.addProperty("oldest_star_id", res.getInt(1));
        json.addProperty("oldest_star_name", res.getString(2));
        json.addProperty("oldest_star_time",
            TimeFormatter.create().withMaxDays(1000).format(res.getDateTime(3)));
      }
    } catch (Exception e) {
      log.error("Error fetching oldest star.", e);
    }

    if (lastSimulation != null) {
      sql = "SELECT COUNT(*) FROM stars"
          + " WHERE empire_count > 0 AND last_simulation >= ?";
      try (SqlStmt stmt = DB.prepare(sql)) {
        stmt.setDateTime(1, lastSimulation);
        SqlResult res = stmt.select();
        while (res.next()) {
          json.addProperty("num_stars", res.getInt(1));
        }
      } catch (Exception e) {
        log.error("Error fetching number of stars to simulate.", e);
      }
    }
  }
}
