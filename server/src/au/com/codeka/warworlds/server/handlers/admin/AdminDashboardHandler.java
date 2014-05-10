package au.com.codeka.warworlds.server.handlers.admin;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.Days;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.StarSimulatorThread;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Star;

public class AdminDashboardHandler extends AdminHandler {
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

        String sql = "SELECT date, active_1d, active_7d, new_signups" +
                    " FROM dashboard_stats" +
                    " ORDER BY date DESC" +
                    " LIMIT 60";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                DateTime dt = new DateTime(rs.getTimestamp(1).getTime());
                int oneDA = rs.getInt(2);
                int sevenDA = rs.getInt(3);
                int newSignups = rs.getInt(4);

                int index = Days.daysBetween(dt, now).getDays();
                if (index < 60) {
                    TreeMap<String, Object> graphEntry = graphData.get(index);
                    graphEntry.put("oneda", oneDA);
                    graphEntry.put("sevenda", sevenDA);
                    graphEntry.put("signups", newSignups);
                }
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }
        data.put("graph_data", graphData);

        try {
            int oldestStarID = StarSimulatorThread.findOldestStar();
            data.put("oldest_star_id", oldestStarID);
            Star oldestStar = new StarController().getStar(oldestStarID);
            data.put("oldest_star_name", oldestStar.getName());
            data.put("oldest_star_simulation_time", oldestStar.getLastSimulation().getMillis());
        } catch (Exception e) {
            throw new RequestException(e);
        }

        render("admin/index.html", data);
    }
}
