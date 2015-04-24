package au.com.codeka.warworlds.server.cron;

import java.sql.SQLException;
import java.util.ArrayList;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.EmpireAltAccounts;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * This cron job runs once per day an searches the database for potential alts. We use this to
 * display the alts on the empire's info screen in the backend.
 */
public class FindAltAccountsCronJob extends CronJob {
  private static final Log log = new Log("FindAltAccountsCronJob");

  @Override
  public void run(String extra) throws Exception {
    ArrayList<String> emailAddresses = new ArrayList<>();

    // First, find all the email addresses of all empires in the database. We look for alts
    // one-by-one.
    String sql = "SELECT DISTINCT user_email FROM empires";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        String emailAddress = res.getString(1);
        emailAddresses.add(emailAddress);
      }
    }

    ArrayList<EmpireAltAccounts> alts = new ArrayList<>();

    sql = "SELECT *, empires.id AS empire_id FROM devices " +
        " INNER JOIN empires ON empires.user_email = devices.user_email" +
        " WHERE device_id IN (" +
        " SELECT device_id FROM devices WHERE user_email = ?" +
        " )";
    try (SqlStmt stmt = DB.prepare(sql)) {
      for (String emailAddress : emailAddresses) {
        stmt.setString(1, emailAddress);
        SqlResult res = stmt.select();

        EmpireAltAccounts.Builder alt_acct_pb = new EmpireAltAccounts.Builder();
        alt_acct_pb.device = new ArrayList<>();
        alt_acct_pb.alt_empire = new ArrayList<>();
        int numFound = 0;
        while (res.next()) {
          String altEmailAddress = res.getString("user_email");
          if (altEmailAddress.equals(emailAddress)) {
            alt_acct_pb.empire_id = res.getInt("empire_id");
          }

          addDeviceInfo(alt_acct_pb, res);
          addEmpireInfo(alt_acct_pb, res);
          numFound++;
        }

        if (numFound > 0) {
          log.info(String.format(
              "Found %d alts and %d devices for: %s", alt_acct_pb.alt_empire.size() - 1,
              alt_acct_pb.device.size(), emailAddress));
          alts.add(alt_acct_pb.build());
        } else {
          log.info("No alts and only one device found for: " + emailAddress);
        }
      }
    }

    sql = "DELETE FROM empire_alts";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.update();
    }

    sql = "INSERT INTO empire_alts (empire_id, alt_blob) VALUES (?, ?)";
    try (SqlStmt stmt = DB.prepare(sql)) {
      for (EmpireAltAccounts pb : alts) {
        stmt.setInt(1, pb.empire_id);
        stmt.setBytes(2, pb.toByteArray());
        stmt.update();
      }
    }
  }

  private void addDeviceInfo(EmpireAltAccounts.Builder alt_acct_pb, SqlResult res)
      throws SQLException {
    // check whether we've added this device already, and add it if not
    String deviceId = res.getString("device_id");
    for (EmpireAltAccounts.DeviceInfo device_info_pb : alt_acct_pb.device) {
      if (device_info_pb.device_id.equals(deviceId)) {
        return;
      }
    }

    EmpireAltAccounts.DeviceInfo.Builder device_info_pb =
        new EmpireAltAccounts.DeviceInfo.Builder();
    device_info_pb.device_id = deviceId;
    device_info_pb.device_build = res.getString("device_build");
    device_info_pb.device_manufacturer = res.getString("device_manufacturer");
    device_info_pb.device_model = res.getString("device_model");
    device_info_pb.device_version = res.getString("device_version");
    alt_acct_pb.device.add(device_info_pb.build());
  }

  private void addEmpireInfo(EmpireAltAccounts.Builder alt_acct_pb, SqlResult res)
      throws SQLException {
    // check whether we've added this empire already, and add it if not
    int empireId = res.getInt("empire_id");
    for (EmpireAltAccounts.EmpireAltEmpire alt_empire_pb : alt_acct_pb.alt_empire) {
      if (alt_empire_pb.empire_id == empireId) {
        return;
      }
    }

    EmpireAltAccounts.EmpireAltEmpire.Builder alt_empire_pb =
        new EmpireAltAccounts.EmpireAltEmpire.Builder();
    alt_empire_pb.empire_id = empireId;
    alt_empire_pb.empire_name = res.getString("name");
    alt_empire_pb.user_email = res.getString("user_email");
    alt_empire_pb.alliance_id = res.getInt("alliance_id");
    alt_acct_pb.alt_empire.add(alt_empire_pb.build());
  }
}
