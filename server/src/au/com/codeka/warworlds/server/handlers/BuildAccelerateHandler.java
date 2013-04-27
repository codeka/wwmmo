package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.BuildQueueController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.BuildRequest;
import au.com.codeka.warworlds.server.model.Star;

public class BuildAccelerateHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        Simulation sim = new Simulation();
        Star star = new StarController().getStar(Integer.parseInt(getUrlParameter("star_id")));
        sim.simulate(star);

        int buildRequestID = Integer.parseInt(getUrlParameter("build_id"));
        int myEmpireID = getSession().getEmpireID();
        float accelerateAmount = 0.5f;
        if (getRequest().getParameter("amount") != null) {
            accelerateAmount = Float.parseFloat(getRequest().getParameter("amount"));
        }

        if (accelerateAmount < 0.5f) {
            accelerateAmount = 0.5f;
        } else if (accelerateAmount > 1.0f) {
            accelerateAmount = 1.0f;
        }

        for (BaseBuildRequest baseBuildRequest : star.getBuildRequests()) {
            BuildRequest buildRequest = (BuildRequest) baseBuildRequest;
            if (buildRequest.getID() == buildRequestID) {
                if (buildRequest.getEmpireID() != myEmpireID) {
                    throw new RequestException(403);
                }

                new BuildQueueController().accelerate(star, buildRequest, accelerateAmount);
                sim.simulate(star);
                new StarController().update(star);
                return;
            }
        }
    }
}
