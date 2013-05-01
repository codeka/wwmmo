package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.model.AllianceJoinRequest;

public class AllianceJoinRequestsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("alliance_id"));
        List<AllianceJoinRequest> joinRequests = new AllianceController().getJoinRequests(allianceID);

        Messages.AllianceJoinRequests.Builder alliance_join_requests_pb = Messages.AllianceJoinRequests.newBuilder();
        for (AllianceJoinRequest joinRequest : joinRequests) {
            Messages.AllianceJoinRequest.Builder alliance_join_request_pb = Messages.AllianceJoinRequest.newBuilder();
            joinRequest.toProtocolBuffer(alliance_join_request_pb);
            alliance_join_requests_pb.addJoinRequests(alliance_join_request_pb);
        }
        setResponseBody(alliance_join_requests_pb.build());
    }

    @Override
    protected void post() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("alliance_id"));
        int empireID = getSession().getEmpireID();
        Messages.AllianceJoinRequest alliance_join_request_pb = getRequestBody(Messages.AllianceJoinRequest.class);

        int joinRequestID = new AllianceController().requestJoin(allianceID, empireID, alliance_join_request_pb.getMessage());

        alliance_join_request_pb = Messages.AllianceJoinRequest.newBuilder()
                                    .setKey(Integer.toString(joinRequestID))
                                    .setAllianceKey(Integer.toString(allianceID))
                                    .setEmpireKey(Integer.toString(empireID))
                                    .setMessage(alliance_join_request_pb.getMessage())
                                    .setTimeRequested(DateTime.now().getMillis() / 1000)
                                    .setState(Messages.AllianceJoinRequest.RequestState.PENDING)
                                    .build();
        setResponseBody(alliance_join_request_pb);
    }

    @Override
    protected void put() throws RequestException {
        // TODO
    }
}
