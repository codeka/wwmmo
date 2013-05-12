package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;

public class EmpiresHandler extends RequestHandler {
    @Override
    protected void put() throws RequestException {
        Messages.Empire empire_pb = getRequestBody(Messages.Empire.class);
        Empire empire = new Empire();
        empire.fromProtocolBuffer(empire_pb);

        empire.setEmailAddr(getSession().getEmail());

        EmpireController ctrl = new EmpireController();
        ctrl.createEmpire(empire);

        // also, update the session with the new empire ID
        String sql = "UPDATE sessions SET empire_id = ? WHERE session_cookie = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, empire.getID());
            stmt.setString(2, getSession().getCookie());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }
}
