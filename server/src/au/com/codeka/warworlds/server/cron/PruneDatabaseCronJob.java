package au.com.codeka.warworlds.server.cron;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class PruneDatabaseCronJob extends CronJob {
  @Override
  public void run(String extra) throws Exception {
    pruneCombatReportsOlderThan(DateTime.now().minusDays(30));
    pruneSitReportsOlderThan(DateTime.now().minusDays(30));
    pruneSessionsOlderThan(DateTime.now().minusDays(7));
  }

  private void pruneCombatReportsOlderThan(DateTime dt) throws Exception {
    String sql = "DELETE FROM combat_reports WHERE end_time < ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setDateTime(1, dt);
      stmt.update();
    }
  }

  private void pruneSitReportsOlderThan(DateTime dt) throws Exception {
    String sql = "DELETE FROM situation_reports WHERE report_time < ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setDateTime(1, dt);
      stmt.update();
    }
  }

  private void pruneSessionsOlderThan(DateTime dt) {
    String sql = "DELETE FROM sessions WHERE login_time < ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setDateTime(1, dt);
      stmt.update();
    } catch (Exception e) {
      // ignore?
    }
  }
}
