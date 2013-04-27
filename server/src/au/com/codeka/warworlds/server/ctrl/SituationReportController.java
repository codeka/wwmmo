package au.com.codeka.warworlds.server.ctrl;

import java.sql.SQLException;

import org.expressme.openid.Base64;
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
            String base64 = Base64.encodeBytes(sitrep_pb.toByteArray());
            new NotificationController().sendNotification(empireID, "sitrep", base64);
        } catch (SQLException e) {
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

        public void saveSituationReport(Messages.SituationReport sitrep_pb) throws SQLException {
            String sql = "INSERT INTO situation_reports (empire_id, star_id, report_time, report)" +
                        " VALUES (?, ?, ?, ?)";
            SqlStmt stmt = prepare(sql);
            stmt.setInt(1, Integer.parseInt(sitrep_pb.getEmpireKey()));
            stmt.setInt(2, Integer.parseInt(sitrep_pb.getStarKey()));
            stmt.setDateTime(3, new DateTime(sitrep_pb.getReportTime() * 1000, DateTimeZone.UTC));
            stmt.setBlob(4, sitrep_pb.toByteArray());
            stmt.update();
        }
    }
}
