package au.com.codeka.warworlds.server.ctrl;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;

public class ErrorReportsController {
    private DataBase db;

    public ErrorReportsController() {
        db = new DataBase();
    }
    public ErrorReportsController(Transaction trans) {
        db = new DataBase(trans);
    }

    public void saveErrorReport(Messages.ErrorReport error_report_pb) {
        try {
            Messages.ErrorReports error_reports_pb = Messages.ErrorReports.newBuilder()
                    .addReports(error_report_pb)
                    .build();
            db.saveErrorReports(error_reports_pb);
        } catch (Exception e) {
            // we just ignore errors here...
        }
    }

    public void saveErrorReports(Messages.ErrorReports error_reports_pb) {
        try {
            db.saveErrorReports(error_reports_pb);
        } catch (Exception e) {
            // we just ignore errors here...
        }
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public void saveErrorReports(Messages.ErrorReports error_reports_pb) throws Exception {
            String sql = "INSERT INTO error_reports (report_date, empire_id, message, exception_class, context, data) " +
                    " VALUES (?, ?, ?, ?, ?, ?)";
            try (SqlStmt stmt = DB.prepare(sql)) {
                for (Messages.ErrorReport pb : error_reports_pb.getReportsList()) {
                    stmt.setDateTime(1, new DateTime(pb.getReportTime()));
                    if (pb.hasEmpireId()) {
                        stmt.setInt(2, pb.getEmpireId());
                    } else {
                        stmt.setNull(2);
                    }
                    stmt.setString(3, pb.getMessage());
                    stmt.setString(4, pb.getExceptionClass());
                    stmt.setString(5, pb.getContext());
                    stmt.setBlob(6, pb.toByteArray());
                    stmt.update();
                }
            }
        }
    }
}
