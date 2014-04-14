package au.com.codeka.warworlds.server.handlers;

import org.apache.xerces.impl.dv.util.Base64;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.model.AllianceRequest;

public class AllianceRequestsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("alliance_id"));

        Integer cursor = null;
        if (getRequest().getParameter("cursor") != null) {
            try {
                cursor = Integer.parseInt(new String(Base64.decode(
                        getRequest().getParameter("cursor")), "utf-8"));
            } catch (Exception e) {
                throw new RequestException(400);
            }
        }

        int minID = 0;
        Messages.AllianceRequests.Builder alliance_requests_pb = Messages.AllianceRequests.newBuilder();
        for (AllianceRequest request : new AllianceController().getRequests(allianceID, false, cursor)) {
            Messages.AllianceRequest.Builder alliance_request_pb = Messages.AllianceRequest.newBuilder();
            request.toProtocolBuffer(alliance_request_pb);
            alliance_requests_pb.addRequests(alliance_request_pb);
            if (minID == 0 || minID > request.getID()) {
                minID = request.getID();
            }
        }

        if (minID != 0) {
            alliance_requests_pb.setCursor(Base64.encode(Integer.toString(minID).getBytes()));
        }

        setResponseBody(alliance_requests_pb.build());
    }

    @Override
    protected void post() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("alliance_id"));
        int empireID = getSession().getEmpireID();
        Messages.AllianceRequest alliance_request_pb = getRequestBody(Messages.AllianceRequest.class);
        if (alliance_request_pb.getAllianceId() != allianceID) {
            throw new RequestException(400, "AllianceID does not match.");
        }
        if (alliance_request_pb.getRequestEmpireId() != empireID) {
            throw new RequestException(400, "EmpireID does not match!");
        }

        AllianceRequest request = new AllianceRequest();
        request.fromProtocolBuffer(alliance_request_pb);
        new AllianceController().addRequest(request);
    }
}
