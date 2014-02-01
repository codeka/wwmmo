package au.com.codeka.warworlds.server.cron;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class PurgeSessionsCronJob extends CronJob {
    @Override
    public void run(String extra) throws Exception {
        DateTime dt = DateTime.now().minusDays(extraToNum(extra, 1, 7));
        purgeSessionsOlderThan(dt);
    }

    private void purgeSessionsOlderThan(DateTime dt) {
        String sql = "DELETE FROM sessions WHERE login_time < ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, dt);
            stmt.update();
        } catch (Exception e) {
            // ignore?
        }
    }
}
