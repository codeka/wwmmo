package au.com.codeka.warworlds.server.cron.jobs;

import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

@CronJob(name = "Find non-CTS devices", desc = "Finds and marks devices that not CTS-compliant.")
public class FindNonCtsDevicesCronJob extends AbstractCronJob {
  private static final Log log = new Log("FindNonCtsDevicesCronJob");

  @Override
  public String run(String extra) throws Exception {
    HashSet<String> deviceIds = new HashSet<>();

    long startTime = System.currentTimeMillis();
    String sql =
        "SELECT device_id, empires.id, empires.name " +
        "FROM empire_logins " +
        "INNER JOIN empires " +
        "  ON empire_logins.empire_id = empires.id " +
        "INNER JOIN devices " +
        "  ON devices.user_email = empires.user_email " +
        "  AND devices.device_build = empire_logins.device_build " +
        "WHERE safetynet_basic_integrity=0 " +
        "  AND safetynet_attestation_statement is not null " +
        "  AND safetynet_attestation_statement <> '' ";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult result = stmt.select();
      while (result.next()) {
        String deviceId = result.getString(1);
        int empireId = result.getInt(2);
        String empireName = result.getString(3);

        log.info("Device '%s' from empire [%d] %s failed basic integrity test.",
            deviceId, empireId, empireName);
        deviceIds.add(deviceId);
      }
    }

    sql = "UPDATE devices SET deny_access=?, deny_date=?, deny_reason=? WHERE device_id=?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      for (String deviceId : deviceIds) {
        stmt.setInt(1, 1);
        stmt.setDateTime(2, DateTime.now());
        stmt.setString(3, "Failed basic integrity test");
        stmt.setString(4, deviceId);
        stmt.update();
      }
    }

    return String.format(
        Locale.ENGLISH, "Updated %d devices in %dms.", deviceIds.size(),
        System.currentTimeMillis() - startTime);
  }
}
