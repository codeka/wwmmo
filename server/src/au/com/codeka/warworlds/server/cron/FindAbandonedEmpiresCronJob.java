package au.com.codeka.warworlds.server.cron;

import java.util.ArrayList;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

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
    private static final Logger log = LoggerFactory.getLogger(FindAbandonedEmpiresCronJob.class);

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
                     " WHERE state = " + Empire.State.ACTIVE.getValue() +
                       " AND last_login < DATE_ADD(NOW(), INTERVAL -14 DAY)" +
                       " AND num_stars <= 1";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                int empireID = res.getInt(1);
                String empireName = res.getString(2);
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

        updateAbandonedStars();
    }

    private void updateAbandonedStars() throws Exception {
        String sql = "SELECT stars.id, empires.id" +
                     " FROM stars" +
                     " INNER JOIN empire_presences ON empire_presences.star_id = stars.id" + 
                     " INNER JOIN empires ON empires.id = empire_presences.empire_id" + 
                     " WHERE empires.state = " + Empire.State.ABANDONED.getValue();
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                int starID = res.getInt(1);
                int empireID = res.getInt(2);
                try {
                    updateAbandonedStar(starID, empireID);
                } catch (Exception e) {
                    log.error("Error marking star abandoned, starID="+starID, e);
                }
            }
        }
    }

    private void updateAbandonedStar(int starID, int empireID) throws Exception {
        Star star = new StarController().getStar(starID);
        float distanceToNonAbandonedEmpire = 0.0f;

        // find all stars around us with non-abandoned empires on them
        String sql = "SELECT sectors.x, sectors.y, stars.x, stars.y" +
                     " FROM stars" +
                     " INNER JOIN sectors ON sectors.id = stars.sector_id" +
                     " INNER JOIN empire_presences ON empire_presences.star_id = stars.id" +
                     " INNER JOIN empires ON empire_presences.empire_id = empires.id" +
                     " WHERE sectors.x < ? AND sectors.x > ?" +
                       " AND sectors.y < ? AND sectors.y > ?" +
                       " AND empires.state = " + Empire.State.ACTIVE.getValue();
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setLong(1, star.getSectorX() + 3);
            stmt.setLong(2, star.getSectorX() - 3);
            stmt.setLong(3, star.getSectorY() + 3);
            stmt.setLong(4, star.getSectorY() - 3);
            SqlResult res = stmt.select();

            while (res.next()) {
                long sectorX = res.getLong(1);
                long sectorY = res.getLong(2);
                int offsetX = res.getInt(3);
                int offsetY = res.getInt(4);

                float distance = Sector.distanceInParsecs(star, sectorX, sectorY, offsetX, offsetY);
                if (distanceToNonAbandonedEmpire < 0.001f || distance < distanceToNonAbandonedEmpire) {
                    distanceToNonAbandonedEmpire = distance;
                }
            }
        }

        if (distanceToNonAbandonedEmpire < 0.0001f) {
            distanceToNonAbandonedEmpire = 9999.0f;
        }

        double distanceToCentre = Math.sqrt((star.getSectorX() * star.getSectorX()) + (star.getSectorY() * star.getSectorY()));

        sql = "INSERT INTO abandoned_stars (star_id, empire_id, distance_to_centre, distance_to_non_abandoned_empire)" +
              " VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE" +
              " distance_to_non_abandoned_empire = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getID());
            stmt.setInt(2, empireID);
            stmt.setDouble(3, distanceToCentre);
            stmt.setDouble(4, distanceToNonAbandonedEmpire);
            stmt.setDouble(5, distanceToNonAbandonedEmpire);
            stmt.update();
        }
    }
}
