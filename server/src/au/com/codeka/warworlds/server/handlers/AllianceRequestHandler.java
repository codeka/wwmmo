package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.AllianceRequestVote;

public class AllianceRequestHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("alliance_id"));
        int requestID = Integer.parseInt(getUrlParameter("request_id"));
        int empireID = getSession().getEmpireID();

        Messages.AllianceRequestVote vote_pb = getRequestBody(Messages.AllianceRequestVote.class);
        if (vote_pb.getAllianceRequestId() != requestID) {
            throw new RequestException(400, "RequestID does not match.");
        }
        if (vote_pb.getAllianceId() != allianceID) {
            throw new RequestException(400, "AllianceID does not match.");
        }
        if (vote_pb.hasEmpireId() && vote_pb.getEmpireId() != empireID) {
            throw new RequestException(400, "EmpireID does not match.");
        }

        AllianceRequestVote requestVote = new AllianceRequestVote();
        requestVote.fromProtocolBuffer(vote_pb);
        requestVote.setEmpireID(empireID);

        try (Transaction t = DB.beginTransaction()) {
            new AllianceController().vote(requestVote);
            t.commit();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }
}
