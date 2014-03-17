package au.com.codeka.warworlds.server.cron;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

public class FixWormholesCronJob extends CronJob {
    private static final Logger log = LoggerFactory.getLogger(FixWormholesCronJob.class);

    @Override
    public void run(String extra) throws Exception {
        String sql = "SELECT * FROM stars WHERE star_type=7 AND" +
                    " id NOT IN (SELECT target_star_id FROM fleets WHERE state=2)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                updateStar(rs);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void updateStar(ResultSet rs) {
        try {
            Star s = new StarController().getStar(rs.getInt("id"));

            s.setStarType(Star.getStarType(Star.Type.Wormhole));
            s.setName("Wormhole");
            s.setWormholeExtra(new Star.WormholeExtra(findEmpire(s)));
            new StarController().update(s);

        } catch (RequestException | SQLException e) {
            log.error("", e);
        }
    }

    private int findEmpire(Star star) {
        float distanceToNonAbandonedEmpire = 0.0f;
        int empireID = 0;

        // find all stars around us with non-abandoned empires on them
        String sql = "SELECT sectors.x, sectors.y, stars.x, stars.y, empires.id" +
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
            ResultSet rs = stmt.select();

            while (rs.next()) {
                long sectorX = rs.getLong(1);
                long sectorY = rs.getLong(2);
                int offsetX = rs.getInt(3);
                int offsetY = rs.getInt(4);
                int thisEmpireID = rs.getInt(5);

                float distance = Sector.distanceInParsecs(star, sectorX, sectorY, offsetX, offsetY);
                if (distanceToNonAbandonedEmpire < 0.001f || distance < distanceToNonAbandonedEmpire) {
                    distanceToNonAbandonedEmpire = distance;
                    empireID = thisEmpireID;
                }
            }
        } catch (Exception e) {
            log.error("", e);
            return 0;
        }

        return empireID;
    }
}
