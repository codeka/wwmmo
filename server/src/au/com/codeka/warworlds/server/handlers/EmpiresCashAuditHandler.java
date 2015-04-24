package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import au.com.codeka.common.Wire;
import au.com.codeka.common.protobuf.CashAuditRecord;
import au.com.codeka.common.protobuf.CashAuditRecords;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class EmpiresCashAuditHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int empireID = Integer.parseInt(getUrlParameter("empireid"));
        if (!getSession().isAdmin()) {
            throw new RequestException(403); // TODO: allow you to get your own...
        }

        String sql = "SELECT reason FROM empire_cash_audit WHERE empire_id = ? ORDER BY time DESC";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, empireID);
            SqlResult res = stmt.select();

            CashAuditRecords cash_audit_records_pb = new CashAuditRecords();
            cash_audit_records_pb.records = new ArrayList<>();
            while (res.next()) {
                cash_audit_records_pb.records.add(
                    Wire.i.parseFrom(res.getBytes(1), CashAuditRecord.class));
            }
            setResponseBody(cash_audit_records_pb);
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }
}
