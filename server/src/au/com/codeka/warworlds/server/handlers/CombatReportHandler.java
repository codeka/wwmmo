package au.com.codeka.warworlds.server.handlers;

import java.sql.ResultSet;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class CombatReportHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int combatReportID = Integer.parseInt(getUrlParameter("combat_report_id"));

        String sql = "SELECT rounds FROM combat_reports WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, combatReportID);
            ResultSet rs = stmt.select();
            while (rs.next()) {
                Messages.CombatReport combat_report_pb = Messages.CombatReport.parseFrom(rs.getBytes(1));
                setResponseBody(combat_report_pb);
                return;
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }

        throw new RequestException(404);
    }
}
