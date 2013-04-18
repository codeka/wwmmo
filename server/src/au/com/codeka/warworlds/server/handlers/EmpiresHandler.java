package au.com.codeka.warworlds.server.handlers;

import com.google.protobuf.Message;

import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.model.Empire;

public class EmpiresHandler extends RequestHandler {
    @Override
    protected Message put() throws RequestException {
        Messages.Empire empire_pb = getRequestBody(Messages.Empire.class);
        Empire empire = new Empire();
        empire.fromProtocolBuffer(empire_pb);

        empire.setEmailAddr(getCurrentUser());

        Simulation sim = new Simulation();
        EmpireController ctrl = new EmpireController(sim);
        ctrl.createEmpire(empire);

        return null;
    }
}
