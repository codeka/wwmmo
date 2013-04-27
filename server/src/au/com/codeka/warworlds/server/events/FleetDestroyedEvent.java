package au.com.codeka.warworlds.server.events;

import java.sql.ResultSet;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.Event;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Star;

public class FleetDestroyedEvent extends Event {
    private final Logger log = LoggerFactory.getLogger(FleetDestroyedEvent.class);

    @Override
    protected String getNextEventTimeSql() {
        return "SELECT MIN(time_destroyed) FROM fleets";
    }

    @Override
    public void process() {
        String sql = "SELECT id, star_id FROM fleets WHERE time_destroyed < ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, DateTime.now().plusSeconds(10)); // anything in the next 10 seconds is a candidate
            ResultSet rs = stmt.select();
            while (rs.next()) {
                Star star = new StarController().getStar(rs.getInt(2));
                Simulation sim = new Simulation();
                sim.simulate(star);
                new StarController().update(star);
            }
        } catch(Exception e) {
            log.error("Error processing fleet-move event!", e);
            // TODO: errors?
        }
    }
}
