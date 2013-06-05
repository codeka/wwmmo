package au.com.codeka.warworlds.server.handlers;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.BuildQueueController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.BuildRequest;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Star;

public class BuildQueueHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        Messages.BuildRequest build_request_pb = getRequestBody(Messages.BuildRequest.class);
        try (Transaction t = DB.beginTransaction()) {
            Star star = new StarController(t).getStar(Integer.parseInt(build_request_pb.getStarKey()));
            if (star == null) {
                throw new RequestException(404);
            }

            Colony colony = null;
            for (BaseColony c : star.getColonies()) {
                if (c.getKey().equals(build_request_pb.getColonyKey())) {
                    colony = (Colony) c;
                }
            }
            if (colony == null) {
                throw new RequestException(404);
            }
            if (colony.getEmpireID() != getSession().getEmpireID()) {
                throw new RequestException(403);
            }

            new Simulation().simulate(star);

            BuildRequest buildRequest = new BuildRequest();
            buildRequest.fromProtocolBuffer(build_request_pb);
            buildRequest.setPlanetIndex(colony.getPlanetIndex());
            buildRequest.setStartTime(DateTime.now());
            buildRequest.setEndTime(DateTime.now().plusMinutes(5));
            new BuildQueueController(t).build(buildRequest);

            // add the build request to the star and simulate again
            star.getBuildRequests().add(buildRequest);
            new Simulation().simulate(star);
            new StarController(t).update(star);

            t.commit();

            Messages.BuildRequest.Builder build_request_pb_builder = Messages.BuildRequest.newBuilder();
            buildRequest.toProtocolBuffer(build_request_pb_builder);
            setResponseBody(build_request_pb_builder.build());
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }
}
