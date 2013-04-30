package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ScoutReportController;
import au.com.codeka.warworlds.server.model.ScoutReport;

public class ScoutReportsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int starID = Integer.parseInt(getUrlParameter("star_id"));
        List<ScoutReport> reports = new ScoutReportController().getScoutReports(starID, getSession().getEmpireID());

        Messages.ScoutReports.Builder scout_reports_pb = Messages.ScoutReports.newBuilder();
        for (ScoutReport scoutReport : reports) {
            Messages.ScoutReport.Builder scout_report_pb = Messages.ScoutReport.newBuilder();
            scoutReport.toProtocolBuffer(scout_report_pb);
            scout_reports_pb.addReports(scout_report_pb);
        }
        setResponseBody(scout_reports_pb.build());
    }
}
