package au.com.codeka.warworlds.server.handlers;

import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.BuildQueueController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.events.BuildCompleteEvent;
import au.com.codeka.warworlds.server.model.BuildRequest;
import au.com.codeka.warworlds.server.model.Star;

public class BuildAccelerateHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        Simulation sim = new Simulation(new Simulation.LogHandler() {
            @Override
            public void log(String message) {
                //log.info(message);
            }
        });
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

                if (new BuildQueueController().accelerate(star, buildRequest, accelerateAmount)) {
                    // if it's complete, trigger the build complete event now, without going through
                    // the event processor (saves a bit of time)
                    new BuildQueueController().saveBuildRequest(buildRequest);
                    new BuildCompleteEvent().process();
                } else {
                    // if it's not actually complete yet, just simulate the star again
                    sim.simulate(star);
                    try {
                        new StarController().update(star);
                    } catch (MySQLTransactionRollbackException e) {
                        throw new RequestException(e);
                    }
                }
                return;
            }
        }
    }
}
