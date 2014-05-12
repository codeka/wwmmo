package au.com.codeka.warworlds.server.handlers.admin;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.TimeFormatter;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class AdminDashboardHandler extends AdminHandler {
    private static final Logger log = LoggerFactory.getLogger(AdminDashboardHandler.class);

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

        populateOldestStar(data);
        render("admin/index.html", data);
    }

    @Override
    protected void post() throws RequestException {
        if (!isAdmin()) {
            return;
        }

        TreeMap<String, Object> data = new TreeMap<String, Object>();
        populateOldestStar(data);
        setResponseJson(data);
    }

    private void populateOldestStar(Map<String, Object> data) {
        // for reasons that I don't understand (I'm blaming crappy MySQL) this is hella slow
        // if I just try to select them all at once. So doing it like this is slightly faster.
        int starID = 0;
        String sql = "SELECT stars.id FROM stars" +
                " WHERE empire_count > 0" +
                " ORDER BY last_simulation ASC LIMIT 2";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                starID = rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("Error fetching oldest star.", e);
        }

        if (starID > 0) {
            sql = "SELECT id, name, last_simulation FROM stars WHERE id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, starID);
                ResultSet rs = stmt.select();
                if (rs.next()) {
                    data.put("oldest_star_id", rs.getInt(1));
                    data.put("oldest_star_name", rs.getString(2));
                    data.put("oldest_star_time", TimeFormatter.create().withMaxDays(1000).format(
                            new DateTime(rs.getTimestamp(3).getTime())));
                }
            } catch (Exception e) {
                log.error("Error fetching oldest star.", e);
            }
        }
    }
}
