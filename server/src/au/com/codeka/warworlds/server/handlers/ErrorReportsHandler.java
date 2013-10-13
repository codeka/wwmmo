package au.com.codeka.warworlds.server.handlers;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

/** This handler is where error reports from the client are posted. */
public class ErrorReportsHandler extends RequestHandler {
    private static Logger log = LoggerFactory.getLogger(ErrorReportsHandler.class);

    @Override
    public void post() throws RequestException {
        Messages.ErrorReports error_reports_pb = getRequestBody(Messages.ErrorReports.class);

        String sql = "INSERT INTO error_reports (report_date, empire_id, message, data) VALUES (?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            for (Messages.ErrorReport pb : error_reports_pb.getReportsList()) {
                stmt.setDateTime(1, new DateTime(pb.getReportTime()));
                stmt.setInt(2, pb.getEmpireId());
                stmt.setString(3, pb.getMessage());
                stmt.setBlob(4, pb.toByteArray());
                stmt.update();
            }
        } catch(Exception e) {
            // ignore errors...
            log.warn("Exception caught while saving error reports.", e);
        }
    }
}
