package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;

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

        au.com.codeka.common.protobuf.Alliances.Builder alliances_pb =
                new au.com.codeka.common.protobuf.Alliances.Builder();
        alliances_pb.alliances = new ArrayList<>();
        for (Alliance alliance : alliances) {
            au.com.codeka.common.protobuf.Alliance alliance_pb =
                    new au.com.codeka.common.protobuf.Alliance();
            alliance.toProtocolBuffer(alliance_pb);
            alliances_pb.alliances.add(alliance_pb);
        }
        setResponseBody(alliances_pb.build());
    }

    @Override
    protected void post() throws RequestException {
        au.com.codeka.common.protobuf.Alliance alliance_pb =
                getRequestBody(au.com.codeka.common.protobuf.Alliance.class);
        Alliance alliance = new Alliance();
        alliance.fromProtocolBuffer(alliance_pb);

        Empire myEmpire = new EmpireController().getEmpire(getSession().getEmpireID());
        new AllianceController().createAlliance(alliance, myEmpire);
    }
}
