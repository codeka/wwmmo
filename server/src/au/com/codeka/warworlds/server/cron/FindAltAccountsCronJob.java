package au.com.codeka.warworlds.server.cron;

import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * This cron job runs once per day an searches the database for potential
 * alts. We use this to display the alts on the empire's info screen in the
 * backend.
 */
public class FindAltAccountsCronJob extends CronJob {
    private static final Logger log = LoggerFactory.getLogger(FindAltAccountsCronJob.class);

    @Override
    public void run(String extra) throws Exception {
        ArrayList<String> emailAddrs = new ArrayList<String>();

        // First, find all the email addresses of all empires
        // in the database. We look for alts one-by-one.
        String sql = "SELECT DISTINCT user_email FROM empires";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                String emailAddr = res.getString(1);
                emailAddrs.add(emailAddr);
            }
        }

        ArrayList<Messages.EmpireAltAccounts> alts = new ArrayList<Messages.EmpireAltAccounts>();

        sql = "SELECT *, empires.id AS empire_id FROM devices "+
             " INNER JOIN empires ON empires.user_email = devices.user_email" +
             " WHERE device_id IN (" +
                " SELECT device_id FROM devices WHERE user_email = ?" +
             " )";
        try (SqlStmt stmt = DB.prepare(sql)) {
            for (String emailAddr : emailAddrs) {
                stmt.setString(1, emailAddr);
                SqlResult res = stmt.select();

                Messages.EmpireAltAccounts.Builder alt_acct_pb = Messages.EmpireAltAccounts.newBuilder();
                int numFound = 0;
                while (res.next()) {
                    String altEmailAddr = res.getString("user_email");
                    if (altEmailAddr.equals(emailAddr)) {
                        alt_acct_pb.setEmpireId(res.getInt("empire_id"));
                    }

                    addDeviceInfo(alt_acct_pb, res);
                    addEmpireInfo(alt_acct_pb, res);
                    numFound ++;
                }

                if (numFound > 0) {
                    log.info(String.format("Found %d alts and %d devices for: %s",
                            alt_acct_pb.getAltEmpireCount() - 1,
                            alt_acct_pb.getDeviceCount(),
                            emailAddr));
                    alts.add(alt_acct_pb.build());
                } else {
                    log.info("No alts and only one device found for: " + emailAddr);
                }
            }
        }

        sql = "DELETE FROM empire_alts";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.update();
        }

        sql = "INSERT INTO empire_alts (empire_id, alt_blob) VALUES (?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            for (Messages.EmpireAltAccounts pb : alts) {
                stmt.setInt(1, pb.getEmpireId());
                stmt.setBytes(2, pb.toByteArray());
                stmt.update();
            }
        }
    }

    private void addDeviceInfo(Messages.EmpireAltAccounts.Builder alt_acct_pb, SqlResult res)
            throws SQLException {
        // check whether we've added this device already, and add it if not
        String deviceId = res.getString("device_id");
        for (Messages.EmpireAltAccounts.DeviceInfo device_info_pb : alt_acct_pb.getDeviceList()) {
            if (device_info_pb.getDeviceId().equals(deviceId)) {
                return;
            }
        }

        Messages.EmpireAltAccounts.DeviceInfo.Builder device_info_pb =
                Messages.EmpireAltAccounts.DeviceInfo.newBuilder();
        device_info_pb.setDeviceId(deviceId);
        device_info_pb.setDeviceBuild(res.getString("device_build"));
        device_info_pb.setDeviceManufacturer(res.getString("device_manufacturer"));
        device_info_pb.setDeviceModel(res.getString("device_model"));
        device_info_pb.setDeviceVersion(res.getString("device_version"));
        alt_acct_pb.addDevice(device_info_pb);
    }

    private void addEmpireInfo(Messages.EmpireAltAccounts.Builder alt_acct_pb, SqlResult res)
            throws SQLException {
        // check whether we've added this device already, and add it if not
        int empireId = res.getInt("empire_id");
        for (Messages.EmpireAltAccounts.EmpireAltEmpire alt_empire_pb : alt_acct_pb.getAltEmpireList()) {
            if (alt_empire_pb.getEmpireId() == empireId) {
                return;
            }
        }

        Messages.EmpireAltAccounts.EmpireAltEmpire.Builder alt_empire_pb =
                Messages.EmpireAltAccounts.EmpireAltEmpire.newBuilder();
        alt_empire_pb.setEmpireId(empireId);
        alt_empire_pb.setEmpireName(res.getString("name"));
        alt_empire_pb.setUserEmail(res.getString("user_email"));
        Integer allianceID = res.getInt("alliance_id");
        if (allianceID != null) {
            alt_empire_pb.setAllianceId(allianceID);
        }
        alt_acct_pb.addAltEmpire(alt_empire_pb);
    }
}
