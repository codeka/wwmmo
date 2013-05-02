package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;

public class AllianceMembersHandler extends RequestHandler {
    @Override
    protected void delete() throws RequestException {
        Messages.AllianceLeaveRequest leave_request_pb = getRequestBody(Messages.AllianceLeaveRequest.class);
        if (Integer.parseInt(leave_request_pb.getEmpireKey()) != getSession().getEmpireID()) {
            throw new RequestException (403);
        }

        new AllianceController().leaveAlliance(getSession().getEmpireID(), getSession().getAllianceID());
    }
}
