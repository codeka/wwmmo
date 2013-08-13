package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;

public class SituationReportController {
    private DataBase db;

    public SituationReportController() {
        db = new DataBase();
    }
    public SituationReportController(Transaction trans) {
        db = new DataBase(trans);
    }

    public void saveSituationReport(Messages.SituationReport sitrep_pb) throws RequestException {
        try {
            db.saveSituationReport(sitrep_pb);

            int empireID = Integer.parseInt(sitrep_pb.getEmpireKey());
            String base64 = Base64.encodeBase64String(sitrep_pb.toByteArray());
            new NotificationController().sendNotificationToEmpire(empireID, "sitrep", base64);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public List<Messages.SituationReport> fetch(Integer empireID, Integer starID, DateTime after,
                                                int limit) throws RequestException {
        try {
            return db.fetch(empireID, starID, after, limit);
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public void saveSituationReport(Messages.SituationReport sitrep_pb) throws Exception {
            String sql = "INSERT INTO situation_reports (empire_id, star_id, report_time, report)" +
                        " VALUES (?, ?, ?, ?)";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, Integer.parseInt(sitrep_pb.getEmpireKey()));
                stmt.setInt(2, Integer.parseInt(sitrep_pb.getStarKey()));
                stmt.setDateTime(3, new DateTime(sitrep_pb.getReportTime() * 1000, DateTimeZone.UTC));
                stmt.setBlob(4, sitrep_pb.toByteArray());
                stmt.update();
            }
        }

        public List<Messages.SituationReport> fetch(Integer empireID, Integer starID, DateTime after,
                                                    int limit) throws Exception {
            String sql = "SELECT report" +
                        " FROM situation_reports" +
                        " WHERE report_time < ?" +
                        (empireID == null ? "" : " AND empire_id = ?") +
                        (starID == null ? "" : " AND star_id = ?") +
                        " ORDER BY report_time DESC" +
                        " LIMIT "+limit;
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setDateTime(1, after);
                int i = 2;
                if (empireID != null) {
                    stmt.setInt(i, empireID);
                    i++;
                }
                if (starID != null) {
                    stmt.setInt(i, starID);
                    i++;
                }
                ResultSet rs = stmt.select();

                ArrayList<Messages.SituationReport> reports = new ArrayList<Messages.SituationReport>();
                while (rs.next()) {
                    reports.add(Messages.SituationReport.parseFrom(rs.getBytes(1)));
                }
                return reports;
            }
        }
    }
}
