package au.com.codeka.warworlds.server.cron;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * Updates the tables used to render the admin dashboard.
 */
public class UpdateDashboardCronJob extends CronJob {

    @Override
    public void run(String extra) throws Exception {
        DateTime dt = DateTime.now();
        if (extra != null && extra.length() > 0) {
            dt = dt.minusDays(Integer.parseInt(extra));
        }

        int oneDA = 0;
        int sevenDA = 0;
        int newSignups = 0;

        String sql = "SELECT DATE(date) AS date, COUNT(DISTINCT empire_id)," +
                     " (SELECT COUNT(DISTINCT empire_id) FROM empire_logins AS sub WHERE sub.date BETWEEN DATE_SUB(DATE(l.date), INTERVAL 6 DAY) AND DATE_ADD(DATE(l.date), INTERVAL 1 DAY))" +
                     " FROM empire_logins l" +
                     " WHERE DATE(date) = ?" +
                     " GROUP BY DATE(date)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, new DateMidnight(dt));
            SqlResult res = stmt.select();
            if (res.next()) {
                oneDA = res.getInt(2);
                sevenDA = res.getInt(3);
            }
        }

        sql = "SELECT COUNT(*)" +
                " FROM empires" +
                " WHERE signup_date IS NOT NULL" +
                  " AND DATE(signup_date) = ?" +
                " GROUP BY DATE(signup_date)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, new DateMidnight(dt));
            SqlResult res = stmt.select();
            if (res.next()) {
                newSignups = res.getInt(1);
            }
        }

        sql = "INSERT INTO dashboard_stats (date, active_1d, active_7d, new_signups)" +
             " VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE" +
             " active_1d = ?, active_7d = ?, new_signups = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, new DateMidnight(dt));
            stmt.setInt(2, oneDA);
            stmt.setInt(3, sevenDA);
            stmt.setInt(4, newSignups);
            stmt.setInt(5, oneDA);
            stmt.setInt(6, sevenDA);
            stmt.setInt(7, newSignups);
            stmt.update();
        }
    }

}
