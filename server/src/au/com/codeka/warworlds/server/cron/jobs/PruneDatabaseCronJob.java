package au.com.codeka.warworlds.server.cron.jobs;

import org.joda.time.DateTime;

import java.util.Locale;

import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

@CronJob(name = "Prune Database", desc = "Prunes the database of old data.")
public class PruneDatabaseCronJob extends AbstractCronJob {
  @Override
  public String run(String extra) throws Exception {
    int numCombatReports = pruneCombatReportsOlderThan(DateTime.now().minusDays(30));
    int numSitReports = pruneSitReportsOlderThan(DateTime.now().minusDays(30));
    int numSessions = pruneSessionsOlderThan(DateTime.now().minusDays(7));

    return String.format(Locale.ENGLISH,
        "Deleted: %d combat reports, %d sit reports, %d old sessions",
        numCombatReports, numSitReports, numSessions);
  }

  private int pruneCombatReportsOlderThan(DateTime dt) throws Exception {
    String sql = "DELETE FROM combat_reports WHERE end_time < ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setDateTime(1, dt);
      return stmt.update();
    }
  }

  private int pruneSitReportsOlderThan(DateTime dt) throws Exception {
    String sql = "DELETE FROM situation_reports WHERE report_time < ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setDateTime(1, dt);
      return stmt.update();
    }
  }

  private int pruneSessionsOlderThan(DateTime dt)  throws Exception {
    String sql = "DELETE FROM sessions WHERE login_time < ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setDateTime(1, dt);
      return stmt.update();
    }
  }
}
