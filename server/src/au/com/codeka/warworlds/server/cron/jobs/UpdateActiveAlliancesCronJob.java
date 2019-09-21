package au.com.codeka.warworlds.server.cron.jobs;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Map;
import java.util.TreeMap;

import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * Goes through all the alliances and marks inactive any alliance whose member has not logged in for
 * a while.
 */
@CronJob(
    name = "Update Active Alliances",
    desc = "Updates the alliances and marks inactive ones as such.")
public class UpdateActiveAlliancesCronJob extends AbstractCronJob {
  /**
   * If no empire has logged in within this number of days, we'll consider the alliance inactive.
   */
  private long ACTIVE_DAYS_SINCE_LOGIN = 60;

  @Override
  public String run(String extra) throws Exception {
    Map<Integer, DateTime> allianceLastLogins = new TreeMap<>();
    String sql = "SELECT" +
        "  alliances.id," +
        "  MAX(empires.last_login) as last_login" +
        " FROM alliances alliances" +
        " INNER JOIN (" +
        "   SELECT" +
        "    empires.id," +
        "    empires.alliance_id," +
        "    logins.last_login" +
        "   FROM empires empires" +
        "   INNER JOIN (" +
        "    SELECT" +
        "      empire_id," +
        "      MAX(date) AS last_login" +
        "      FROM empire_logins" +
        "      GROUP BY empire_id" +
        "   ) logins ON logins.empire_id = empires.id" +
        " ) empires" +
        " ON alliances.id = empires.alliance_id" +
        " GROUP BY alliances.id";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int allianceID = res.getInt(1);
        DateTime lastLogin = res.getDateTime(2);
        allianceLastLogins.put(allianceID, lastLogin);
      }
    }

    try (SqlStmt stmt = DB.prepare("UPDATE alliances SET is_active = ? WHERE id = ?")) {
      for (Map.Entry<Integer, DateTime> entry : allianceLastLogins.entrySet()) {
        DateTime lastLogin = entry.getValue();
        Days daysSinceLogin = Days.daysBetween(lastLogin, DateTime.now());
        if (daysSinceLogin.getDays() > ACTIVE_DAYS_SINCE_LOGIN) {
          stmt.setInt(1, 0);
        } else {
          stmt.setInt(1, 1);
        }
        stmt.setInt(2, entry.getKey());
        stmt.update();
      }
    }

    return "Success.";
  }
}
