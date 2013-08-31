package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * Handler that handles /realms/.../sit-reports/read.
 */
public class SitReportsReadHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        String sql = "UPDATE empires SET last_sitrep_read_time = NOW() WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, getSession().getEmpireID());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException( e);
        }
    }
}
