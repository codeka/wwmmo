package au.com.codeka.warworlds.server.handlers.admin;

import java.util.TreeMap;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.common.protoformat.PbFormatter;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class AdminEmpireAltsHandler extends AdminHandler {
    @Override
    protected void get() throws RequestException {
        if (!isAdmin()) {
            return;
        }
        TreeMap<String, Object> data = new TreeMap<String, Object>();

        if (getRequest().getParameter("empire_id") != null) {
            int empireID = Integer.parseInt(getRequest().getParameter("empire_id"));
            data.put("empire_id", empireID);

            String sql = "SELECT alt_blob FROM empire_alts WHERE empire_id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, empireID);
                SqlResult res = stmt.select();
                if (res.next()) {
                    byte[] blob = res.getBytes(1);
                    Messages.EmpireAltAccounts pb = Messages.EmpireAltAccounts.parseFrom(blob);
                    data.put("alts", PbFormatter.toJson(pb));
                }
            } catch(Exception e) {
                // TODO: handle errors
            }
        }

        render("admin/empire/alts.html", data);
    }
}
