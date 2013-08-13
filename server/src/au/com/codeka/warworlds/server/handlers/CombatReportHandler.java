package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.CombatReportController;

public class CombatReportHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int combatReportID = Integer.parseInt(getUrlParameter("combat_report_id"));

        Messages.CombatReport combat_report_pb = new CombatReportController().fetchCombatReportPb(
                combatReportID);
        if (combat_report_pb != null) {
            setResponseBody(combat_report_pb);
        } else {
            throw new RequestException(404);
        }
    }
}
