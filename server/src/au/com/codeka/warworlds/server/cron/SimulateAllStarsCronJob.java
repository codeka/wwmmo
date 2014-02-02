package au.com.codeka.warworlds.server.cron;

import java.sql.ResultSet;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Star;

/** This job runs multiple times a day to simulate all "interesting" stars in the galaxy. */
public class SimulateAllStarsCronJob extends CronJob {
    private static final Logger log = LoggerFactory.getLogger(SimulateAllStarsCronJob.class);

    @Override
    public void run(String extra) {
        DateTime dt = DateTime.now().minusHours(extraToNum(extra, 0, 6));
        simulateStarsOlderThan(dt);
    }

    private void simulateStarsOlderThan(DateTime dt) {
        // this'll be a number, 0..6. We want to try & spread out the load throughout the whole
        // day, and this will ensure an individual star is only elligible once every 6 hours
        int mod = dt.getHourOfDay() / 4;
        while (true) {
            ArrayList<Integer> starIDs = new ArrayList<Integer>();
            String sql = "SELECT id FROM stars WHERE last_simulation < ? AND" +
                        " (SELECT COUNT(*) FROM colonies WHERE star_id = stars.id) > 0" +
                        " AND (id % 6 = "+mod+")" +
                        " LIMIT 25";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setDateTime(1, dt);
                ResultSet rs = stmt.select();
                while (rs.next()) {
                    starIDs.add(rs.getInt(1));
                }
            } catch (Exception e) {
            }

            if (starIDs.size() == 0) {
                break;
            }

            int[] ids = new int[starIDs.size()];
            for (int i = 0; i < starIDs.size(); i++) {
                ids[i] = starIDs.get(i);
            }

            try {
                Simulation sim = new Simulation();
                for (Star star : new StarController().getStars(ids)) {
                    sim.simulate(star);
                    new StarController().update(star);
                }
            } catch(Exception e) {
                log.error("Unhandled exception simulating star", e);
            }
        }
    }
}
