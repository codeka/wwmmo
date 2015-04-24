package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.SituationReport;
import au.com.codeka.common.protobuf.SituationReportFilter;
import au.com.codeka.common.protobuf.SituationReports;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.SituationReportController;
import au.com.codeka.warworlds.server.model.Empire;

/**
 * Handler that handles both /realms/.../sit-reports and /realms/.../stars/<star_id>/sit-reports.
 */
public class SitReportsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        String starKey = getUrlParameter("starid");
        Integer starID = starKey == null
                       ? null : Integer.parseInt(starKey);

        // the cursor is basically just a base64-encoding of the last one we returned
        DateTime before = DateTime.now().plusHours(1);
        String cursor = getRequest().getParameter("cursor");
        if (cursor != null && !cursor.isEmpty()) {
            before = new DateTime(Long.parseLong(cursor), DateTimeZone.UTC);
        }

        DateTime after = null;
        if (getRequest().getParameter("show-old-items") == null) {
            Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
            DateTime lastReadTime = empire.getLastSitrepReadTime();
            if (lastReadTime != null) {
                after = lastReadTime;
            }
        }

        SituationReportFilter filter = null;
        if (getRequest().getParameter("filter") != null) {
            filter = SituationReportFilter.valueOf(getRequest().getParameter("filter"));
        }

        Integer empireID = getSession().getEmpireID();
        if (getSession().isAdmin()) {
            empireID = null;
        }
        List<SituationReport> sitreps = new SituationReportController().fetch(
                empireID, starID, before, after, filter, 50);

        DateTime first = null;
        if (!sitreps.isEmpty()) {
            first = new DateTime(sitreps.get(sitreps.size() - 1).report_time * 1000,
                    DateTimeZone.UTC);
        }

        SituationReports sitreps_pb = new SituationReports();
        sitreps_pb.situation_reports = new ArrayList<>();
        for (SituationReport sitrep : sitreps) {
            sitreps_pb.situation_reports.add(sitrep);
        }

        if (first == null) {
            sitreps_pb.cursor = "";
        } else {
            sitreps_pb.cursor = Long.toString(first.getMillis());
        }

        setResponseBody(sitreps_pb);
    }
}
