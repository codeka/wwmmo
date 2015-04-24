package au.com.codeka.warworlds.server.handlers;

import com.google.common.io.BaseEncoding;

import java.util.ArrayList;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.model.AllianceRequest;

public class AllianceRequestsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("allianceid"));

        Integer cursor = null;
        if (getRequest().getParameter("cursor") != null) {
            try {
                cursor = Integer.parseInt(new String(BaseEncoding.base64().decode(
                        getRequest().getParameter("cursor")), "utf-8"));
            } catch (Exception e) {
                throw new RequestException(400);
            }
        }

        int minID = 0;
        au.com.codeka.common.protobuf.AllianceRequests.Builder alliance_requests_pb =
                new au.com.codeka.common.protobuf.AllianceRequests.Builder();
        alliance_requests_pb.requests = new ArrayList<>();
        for (AllianceRequest request : new AllianceController().getRequests(allianceID, false, cursor)) {
            au.com.codeka.common.protobuf.AllianceRequest alliance_request_pb =
                    new au.com.codeka.common.protobuf.AllianceRequest();
            request.toProtocolBuffer(alliance_request_pb);
            alliance_requests_pb.requests.add(alliance_request_pb);
            if (minID == 0 || minID > request.getID()) {
                minID = request.getID();
            }
        }

        if (minID != 0) {
            alliance_requests_pb.cursor = BaseEncoding.base64().encode(
                    Integer.toString(minID).getBytes());
        }

        setResponseBody(alliance_requests_pb.build());
    }

    @Override
    protected void post() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("allianceid"));
        int empireID = getSession().getEmpireID();
        au.com.codeka.common.protobuf.AllianceRequest alliance_request_pb =
                getRequestBody(au.com.codeka.common.protobuf.AllianceRequest.class);
        if (alliance_request_pb.alliance_id != allianceID) {
            throw new RequestException(400, "AllianceID does not match.");
        }
        if (alliance_request_pb.request_empire_id != empireID) {
            throw new RequestException(400, "EmpireID does not match!");
        }

        AllianceRequest request = new AllianceRequest();
        request.fromProtocolBuffer(alliance_request_pb);
        new AllianceController().addRequest(request);
    }
}
