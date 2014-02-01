package au.com.codeka.warworlds.server.cron;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;

/**
 * An abandoned empire is one where the user hasn't logged in for a while, and they only have one star under
 * their control.
 * 
 * Abandoned empires have two things happen to them:
 *  1. their star becomes available for new signups and
 *  2. their empire name becomes available for new signups.
 *
 * If they later log in and their star has been taken, they'll get a standard "empire reset" message with help
 * text explaing that their empire expired. If they log in again and their name has been changed, they'll be
 * required to choose another.
 */
public class FindAbandonedEmpiresCronJob extends CronJob {
    private static final Logger log = LoggerFactory.getLogger(CronJobRegistry.class);

    @Override
    public void run(String extra) throws Exception {
        ArrayList<Integer> abandonedEmpires = new ArrayList<Integer>();

        // first, find all empires not already marked "abandoned" that only have one star and haven't logged
        // in for two weeks
        String sql = "SELECT id, name" +
                     " FROM empires" +
                     " INNER JOIN (" +
                     "SELECT empire_id, MAX(date) AS last_login FROM empire_logins" +
                     " GROUP BY empire_id) logins ON logins.empire_id = empires.id" +
                     " INNER JOIN (" +
                     "SELECT empire_id, COUNT(*) AS num_stars FROM empire_presences" +
                     " GROUP BY empire_id) stars ON stars.empire_id = empires.id" +
                     " WHERE state = 1" + // 1 == ACTIVE
                       " AND last_login < DATE_ADD(NOW(), INTERVAL -14 DAY)" +
                       " AND num_stars <= 1";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(0);
                String empireName = rs.getString(1);
                log.info(String.format(Locale.ENGLISH,
                        "Empire #%d (%s) has not logged in for two weeks, marking abandoned.",
                        empireID, empireName));
                abandonedEmpires.add(empireID);
            }
        }

        sql = "UPDATE empires SET state = ? WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, Empire.State.ABANDONED.getValue());
            for (Integer empireID : abandonedEmpires) {
                stmt.setInt(2, empireID);
                stmt.update();
            }
        }
    }

}
