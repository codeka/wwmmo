package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.model.Alliance;
import au.com.codeka.warworlds.server.model.Empire;

public class AlliancesHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        List<Alliance> alliances = new AllianceController().getAlliances();

        Messages.Alliances.Builder alliances_pb = Messages.Alliances.newBuilder();
        for (Alliance alliance : alliances) {
            Messages.Alliance.Builder alliance_pb = Messages.Alliance.newBuilder();
            alliance.toProtocolBuffer(alliance_pb);
            alliances_pb.addAlliances(alliance_pb);
        }
        setResponseBody(alliances_pb.build());
    }

    @Override
    protected void post() throws RequestException {
        Messages.Alliance alliance_pb = getRequestBody(Messages.Alliance.class);
        Alliance alliance = new Alliance();
        alliance.fromProtocolBuffer(alliance_pb);

        Empire myEmpire = new EmpireController().getEmpire(getSession().getEmpireID());
        new AllianceController().createAlliance(alliance, myEmpire);
    }
}
