package au.com.codeka.warworlds.server.ctrl;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.ErrorReport;
import au.com.codeka.common.protobuf.ErrorReports;
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

    public void saveErrorReport(ErrorReport error_report_pb) {
        try {
            ErrorReports error_reports_pb = new ErrorReports.Builder()
                    .reports(Lists.newArrayList(error_report_pb))
                    .build();
            db.saveErrorReports(error_reports_pb);
        } catch (Exception e) {
            // we just ignore errors here...
        }
    }

    public void saveErrorReports(ErrorReports error_reports_pb) {
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

        public void saveErrorReports(ErrorReports error_reports_pb) throws Exception {
            String sql = "INSERT INTO error_reports (report_date, empire_id, message, exception_class, context, data) " +
                    " VALUES (?, ?, ?, ?, ?, ?)";
            try (SqlStmt stmt = DB.prepare(sql)) {
                for (ErrorReport pb : error_reports_pb.reports) {
                    stmt.setDateTime(1, new DateTime(pb.report_time));
                    stmt.setInt(2, pb.empire_id);
                    stmt.setString(3, pb.message);
                    stmt.setString(4, pb.exception_class);
                    stmt.setString(5, pb.context);
                    stmt.setBytes(6, pb.toByteArray());
                    stmt.update();
                }
            }
        }
    }
}
