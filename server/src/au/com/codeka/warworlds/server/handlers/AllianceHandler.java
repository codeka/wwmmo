package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.model.Alliance;

public class AllianceHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("alliance_id"));
        Alliance alliance = new AllianceController().getAlliance(allianceID, true);

        Messages.Alliance.Builder alliance_pb = Messages.Alliance.newBuilder();
        alliance.toProtocolBuffer(alliance_pb);
        setResponseBody(alliance_pb.build());
    }
}
