package au.com.codeka.warworlds.server.cron;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class PurgeCombatReportsCronJob extends CronJob {
    @Override
    public void run(String extra) throws Exception {
        DateTime dt = DateTime.now().minusDays(extraToNum(extra, 7, 30));
        purgeCombatReportsOlderThan(dt);
    }

    /**
     * Purges (deletes) all combat reports from the database older than the specified date.
     * Essentially, there's little point having combat reports from 6 months ago, and really they
     * just inflate the database.
     */
    public void purgeCombatReportsOlderThan(DateTime dt) throws Exception {
        String sql = "DELETE FROM combat_reports WHERE end_time < ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, dt);
            stmt.update();
        }
    }

}
