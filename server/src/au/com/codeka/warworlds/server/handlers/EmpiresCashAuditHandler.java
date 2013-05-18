package au.com.codeka.warworlds.server.handlers;

import java.sql.ResultSet;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class EmpiresCashAuditHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int empireID = Integer.parseInt(getUrlParameter("empire_id"));
        if (!getSession().isAdmin()) {
            throw new RequestException(403); // TODO: allow you to get your own...
        }

        String sql = "SELECT reason FROM empire_cash_audit WHERE empire_id = ? ORDER BY time DESC";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, empireID);
            ResultSet rs = stmt.select();

            Messages.CashAuditRecords.Builder cash_audit_records_pb = Messages.CashAuditRecords.newBuilder();
            while (rs.next()) {
                cash_audit_records_pb.addRecords(Messages.CashAuditRecord.parseFrom(rs.getBytes(1)));
            }
            setResponseBody(cash_audit_records_pb.build());
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }
}
