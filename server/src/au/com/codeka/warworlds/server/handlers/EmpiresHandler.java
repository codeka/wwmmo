package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.SessionController;
import au.com.codeka.warworlds.server.model.Empire;

public class EmpiresHandler extends RequestHandler {
    @Override
    protected void put() throws RequestException {
        au.com.codeka.common.protobuf.Empire empire_pb =
            getRequestBody(au.com.codeka.common.protobuf.Empire.class);
        Empire empire = new Empire();
        empire.fromProtocolBuffer(empire_pb);

        empire.setEmailAddr(getSession().getEmail());

        EmpireController ctrl = new EmpireController();
        ctrl.createEmpire(empire);

        // also, update the session with the new empire ID
        getSession().setEmpireID(empire.getID());
        new SessionController().saveSession(getSession());
    }
}
