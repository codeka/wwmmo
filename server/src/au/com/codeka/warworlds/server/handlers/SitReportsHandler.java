package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.SituationReportController;

/**
 * Handler that handles both /realms/.../sit-reports and /realms/.../stars/<star_id>/sit-reports.
 */
public class SitReportsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        String starKey = getUrlParameter("star_id");
        Integer starID = starKey == null
                       ? null : Integer.parseInt(starKey);

        // the cursor is basically just a base64-encoding of the last one we returned
        DateTime after = DateTime.now().plusHours(1);
        String cursor = getRequest().getParameter("cursor");
        if (cursor != null && !cursor.isEmpty()) {
            after = new DateTime(Long.parseLong(cursor), DateTimeZone.UTC);
        }

        List<Messages.SituationReport> sitreps = new SituationReportController().fetch(
                getSession().getEmpireID(), starID, after, 100);

        Messages.SituationReports.Builder sitreps_pb = Messages.SituationReports.newBuilder();
        for (Messages.SituationReport sitrep : sitreps) {
            sitreps_pb.addSituationReports(sitrep);
            DateTime thisSitRep = new DateTime(sitrep.getReportTime() * 1000, DateTimeZone.UTC);
            if (thisSitRep.isBefore(after)) {
                after = thisSitRep;
            }
        }

        cursor = Long.toString(after.getMillis());
        sitreps_pb.setCursor(cursor);
        setResponseBody(sitreps_pb.build());
    }
}
