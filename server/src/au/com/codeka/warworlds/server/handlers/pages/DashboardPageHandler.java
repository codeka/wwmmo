package au.com.codeka.warworlds.server.handlers.pages;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.Days;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class DashboardPageHandler extends BasePageHandler {
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

        String sql = "SELECT DATE(date) AS date, COUNT(DISTINCT empire_id)," +
                           " (SELECT COUNT(DISTINCT empire_id) FROM empire_logins AS sub WHERE sub.date BETWEEN DATE_SUB(DATE(l.date), INTERVAL 6 DAY) AND DATE_ADD(DATE(l.date), INTERVAL 1 DAY))" +
                    " FROM empire_logins l" +
                    " GROUP BY DATE(date)" +
                    " ORDER BY date DESC" +
                    " LIMIT 60";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                DateTime dt = new DateTime(rs.getDate(1).getTime());
                int oneDA = rs.getInt(2);
                int sevenDA = rs.getInt(3);

                int index = Days.daysBetween(dt, now).getDays();
                if (index < 60) {
                    TreeMap<String, Object> graphEntry = graphData.get(index);
                    graphEntry.put("oneda", oneDA);
                    graphEntry.put("sevenda", sevenDA);
                }
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }

        sql = "SELECT DATE(signup_date), COUNT(*)" +
             " FROM empires" +
             " WHERE signup_date IS NOT NULL" +
               " AND signup_date > DATE_SUB(DATE(NOW()), INTERVAL 60 DAY)" +
             " GROUP BY DATE(signup_date)" +
             " ORDER BY signup_date DESC";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                DateTime dt = new DateTime(rs.getDate(1).getTime());
                int numSignups = rs.getInt(2);

                int index = Days.daysBetween(dt, now).getDays();
                if (index < 60) {
                    TreeMap<String, Object> graphEntry = graphData.get(index);
                    graphEntry.put("signups", numSignups);
                }
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }

        data.put("graph_data", graphData);

        ArrayList<TreeMap<String, Object>> empireRanks = new ArrayList<TreeMap<String, Object>>();
        sql = "SELECT *" +
             " FROM empire_ranks" +
             " INNER JOIN empires ON empire_ranks.empire_id = empires.id" +
             " ORDER BY rank ASC" +
             " LIMIT 10";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int rank = rs.getInt("rank");
                String empireName = rs.getString("name");
                int totalStars = rs.getInt("total_stars");
                int totalColonies = rs.getInt("total_colonies");
                int totalBuildings = rs.getInt("total_buildings");
                int totalShips = rs.getInt("total_ships");
                int totalPopulation = rs.getInt("total_population");

                TreeMap<String, Object> empireRank = new TreeMap<String, Object>();
                empireRank.put("rank", rank);
                empireRank.put("empireName", empireName);
                empireRank.put("totalStars", totalStars);
                empireRank.put("totalColonies", totalColonies);
                empireRank.put("totalBuildings", totalBuildings);
                empireRank.put("totalShips", totalShips);
                empireRank.put("totalPopulation", totalPopulation);
                empireRanks.add(empireRank);
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }
        data.put("empire_ranks", empireRanks);

        render("admin/index.html", data);
    }
}
