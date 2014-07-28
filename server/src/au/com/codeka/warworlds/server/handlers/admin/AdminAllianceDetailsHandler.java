package au.com.codeka.warworlds.server.handlers.admin;

import java.sql.SQLException;
import java.util.ArrayList;

import com.google.gson.Gson;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * This handler is used by the "alliance search" page for display details about an alliance. We use
 * the default handlers as much as possible, but some things (such as audit history) is not
 * available to non-Admin players, so we have to do it here.
 */
public class AdminAllianceDetailsHandler extends AdminHandler {
    @Override
    protected void get() throws RequestException {
        if (getRequest().getParameter("section").equals("audit")) {
            getAudit();
        } else {
            throw new RequestException(501);
        }
    }

    private void getAudit() throws RequestException {
        String sql = "SELECT alliance_request_id, empire_id, date, amount_before, amount_after, message" +
                    " FROM alliance_bank_balance_audit" +
                    " INNER JOIN alliance_requests on alliance_bank_balance_audit.alliance_request_id = alliance_requests.id" +
                    " WHERE alliance_requests.alliance_id = ?" +
                    " ORDER BY date DESC";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, Integer.parseInt(getUrlParameter("alliance_id")));
            SqlResult result = stmt.select();

            ArrayList<BankBalanceAudit> audit = new ArrayList<BankBalanceAudit>();
            while (result.next()) {
                audit.add(new BankBalanceAudit(result));
            }
            Gson gson = new Gson();
            writeJson(gson.toJsonTree(audit));
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    /** Object that's converted to JSON as the response to the audit request. */
    @SuppressWarnings("unused") // the fields ARE used, by Gson, reflectively
    private static class BankBalanceAudit {
        public long alliance_request_id;
        public long empire_id;
        public long date;
        public double amount_before;
        public double amount_after;
        public String message;

        public BankBalanceAudit(SqlResult result) throws SQLException {
            alliance_request_id = result.getLong("alliance_request_id");
            empire_id = result.getLong("empire_id");
            date = result.getDateTime("date").getMillis();
            amount_before = result.getDouble("amount_before");
            amount_after = result.getDouble("amount_after");
            message = result.getString("message");
        }
    }
}
