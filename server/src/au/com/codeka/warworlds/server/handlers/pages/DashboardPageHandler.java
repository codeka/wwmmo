package au.com.codeka.warworlds.server.handlers.pages;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TreeMap;

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

        ArrayList<TreeMap<String, Object>> ndas = new ArrayList<TreeMap<String, Object>>();
        String sql = "SELECT DATE(date) AS date, COUNT(DISTINCT empire_id)," +
                           " (SELECT COUNT(DISTINCT empire_id) FROM empire_logins AS sub WHERE sub.date BETWEEN DATE_SUB(DATE(l.date), INTERVAL 6 DAY) AND DATE_ADD(DATE(l.date), INTERVAL 1 DAY))" +
                    " FROM empire_logins l" +
                    " GROUP BY DATE(date)" +
                    " ORDER BY date DESC" +
                    " LIMIT 60";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                Date dt = rs.getDate(1);
                int oneDA = rs.getInt(2);
                int sevenDA = rs.getInt(3);

                TreeMap<String, Object> nda = new TreeMap<String, Object>();
                Calendar c = Calendar.getInstance();
                c.setTime(dt);
                nda.put("year", c.get(Calendar.YEAR));
                nda.put("month", c.get(Calendar.MONTH) - 1);
                nda.put("day", c.get(Calendar.DAY_OF_MONTH));
                nda.put("oneda", oneDA);
                nda.put("sevenda", sevenDA);
                ndas.add(nda);
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }
        Collections.reverse(ndas);
        data.put("empire_nda", ndas);

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
