package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.protobuf.ScoutReports;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ScoutReportController;
import au.com.codeka.warworlds.server.model.ScoutReport;

public class ScoutReportsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int starID = Integer.parseInt(getUrlParameter("starid"));
        List<ScoutReport> reports = new ScoutReportController().getScoutReports(starID, getSession().getEmpireID());

        ScoutReports scout_reports_pb = new ScoutReports();
        scout_reports_pb.reports = new ArrayList<>();
        for (ScoutReport scoutReport : reports) {
            au.com.codeka.common.protobuf.ScoutReport scout_report_pb =
                    new au.com.codeka.common.protobuf.ScoutReport();
            scoutReport.toProtocolBuffer(scout_report_pb);
            scout_reports_pb.reports.add(scout_report_pb);
        }
        setResponseBody(scout_reports_pb);
    }
}
