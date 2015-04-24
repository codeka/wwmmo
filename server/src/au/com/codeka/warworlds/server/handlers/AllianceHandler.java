package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.model.Alliance;

public class AllianceHandler extends RequestHandler {
  @Override
  protected void get() throws RequestException {
    int allianceID = Integer.parseInt(getUrlParameter("allianceid"));
    Alliance alliance = new AllianceController().getAlliance(allianceID);

    au.com.codeka.common.protobuf.Alliance alliance_pb =
        new au.com.codeka.common.protobuf.Alliance();
    alliance.toProtocolBuffer(alliance_pb);
    setResponseBody(alliance_pb);
  }
}
