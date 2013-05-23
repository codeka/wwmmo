package au.com.codeka.warworlds.server.handlers.pages;

import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;

/**
 * Handles the /admin/actions/move-star page.
 */
public class ActionsResetEmpirePageHandler extends HtmlPageHandler {
    private final Logger log = LoggerFactory.getLogger(ActionsResetEmpirePageHandler.class);

    @Override
    protected void post() throws RequestException {
        int empireID = Integer.parseInt(getRequest().getParameter("empire_id"));
        String reason = getRequest().getParameter("reason");

        String[] sqls = {
                "DELETE FROM alliance_join_requests WHERE empire_id = ?",
                "DELETE FROM build_requests WHERE empire_id = ?",
                "DELETE FROM buildings WHERE empire_id = ?",
                "DELETE FROM colonies WHERE empire_id = ?",
                "DELETE FROM empire_presences WHERE empire_id = ?",
                "DELETE FROM fleets WHERE empire_id = ?",
                "DELETE FROM scout_reports WHERE empire_id = ?",
                "DELETE FROM situation_reports WHERE empire_id = ?",
            };

        TreeMap<String, Object> data = new TreeMap<String, Object>();
        data.put("complete", true);

        try (Transaction t = DB.beginTransaction()) {
            for (String sql : sqls) {
                try (SqlStmt stmt = t.prepare(sql)) {
                    stmt.setInt(1, empireID);
                    stmt.update();
                }
            }

            String sql = "UPDATE empires SET alliance_id = NULL, cash = 2000, reset_reason = ? WHERE id = ?";
            try (SqlStmt stmt = t.prepare(sql)) {
                stmt.setString(1, reason);
                stmt.setInt(2, empireID);
                stmt.update();
            }

            t.commit();
        } catch (Exception e) {
            log.error("Error resetting empire", e);
            data.put("success", false);
            data.put("msg", e.getMessage());
        }

        data.put("success", true);

        render("admin/actions/reset-empire.html", data);
    }
}
