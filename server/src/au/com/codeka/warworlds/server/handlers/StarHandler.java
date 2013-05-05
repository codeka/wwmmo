package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseScoutReport;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.BuildRequest;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.ScoutReport;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Handles /realm/.../stars/{id} URL
 */
public class StarHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int id = Integer.parseInt(getUrlParameter("star_id"));
        Star star = new StarController().getStar(id);
        if (star == null) {
            throw new RequestException(404);
        }

        int myEmpireID = getSession().getEmpireID();

        // if we don't have any fleets here, remove all the others
        boolean ourFleetExists = false;
        for (BaseFleet baseFleet : star.getFleets()) {
            Fleet fleet = (Fleet) baseFleet;
            if (fleet.getEmpireID() == myEmpireID) {
                ourFleetExists = true;
            }
        }
        if (!ourFleetExists) {
            star.getFleets().clear();
        }

        // remove build requests that aren't ours
        {
            ArrayList<BaseBuildRequest> toRemove = new ArrayList<BaseBuildRequest>();
            for (BaseBuildRequest baseBuildRequest : star.getBuildRequests()) {
                BuildRequest buildRequest = (BuildRequest) baseBuildRequest;
                if (buildRequest.getEmpireID() != myEmpireID) {
                    toRemove.add(baseBuildRequest);
                }
            }
            star.getBuildRequests().removeAll(toRemove);
        }

        // remove all scout reports that aren't ours
        {
            ArrayList<BaseScoutReport> toRemove = new ArrayList<BaseScoutReport>();
            for (BaseScoutReport baseScoutReport : star.getScoutReports()) {
                ScoutReport scoutReport = (ScoutReport) baseScoutReport;
                if (!scoutReport.getEmpireKey().equals(Integer.toString(myEmpireID))) {
                    toRemove.add(baseScoutReport);
                }
            }
            star.getScoutReports().removeAll(toRemove);
        }

        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        star.toProtocolBuffer(star_pb);
        setResponseBody(star_pb.build());
    }
}
