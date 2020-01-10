package au.com.codeka.warworlds.server.handlers;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.BuildQueueController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.events.BuildCompleteEvent;
import au.com.codeka.warworlds.server.model.BuildRequest;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Star;

public class BuildQueueHandler extends RequestHandler {
  @Override
  protected void post() throws RequestException {
    Messages.BuildRequest build_request_pb = getRequestBody(Messages.BuildRequest.class);
    try {
      tryPost(build_request_pb);
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  private void tryPost(Messages.BuildRequest build_request_pb) throws Exception {
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
      if (colony == null || colony.getEmpireID() == null) {
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

      if (build_request_pb.getAccelerateImmediately()) {
        // This actually directly adds the building/fleet, skipping all the stuff that saves the
        // build request, simulates the star, finishes the request, simulates and save the star
        // again.
        new BuildQueueController(t).ensureBuildAllowed(star, buildRequest);

        // If we're accelerating immediately, skip all the build request stuff, just take their
        // cash an add the building/fleet.
        float cost = buildRequest.getDesign().getBuildCost().getCostInMinerals()
            * buildRequest.getCount();

        Messages.CashAuditRecord.Builder audit_record_pb = Messages.CashAuditRecord.newBuilder();
        audit_record_pb.setEmpireId(buildRequest.getEmpireID());
        audit_record_pb.setBuildDesignId(buildRequest.getDesignID());
        audit_record_pb.setBuildCount(buildRequest.getCount());
        audit_record_pb.setAccelerateAmount(1.0f);
        if (!new EmpireController(t).withdrawCash(
            buildRequest.getEmpireID(), cost, audit_record_pb)) {
          throw new RequestException(400, Messages.GenericError.ErrorCode.InsufficientCash,
              "You don't have enough cash to accelerate this build.");
        }

        new BuildCompleteEvent().processImmediateBuildRequest(
            star, colony, buildRequest.getEmpireID(), buildRequest.getDesign().getID(),
            buildRequest.getDesignKind(), buildRequest.getCount(), buildRequest.getNotes(),
            buildRequest.getExistingBuildingID(), buildRequest.getExistingFleetID(),
            buildRequest.getUpgradeID());
      } else {
        new BuildQueueController(t).build(buildRequest);

        // add the build request to the star and simulate again
        star.getBuildRequests().add(buildRequest);
        new Simulation().simulate(star);
        new StarController(t).update(star);
      }

      t.commit();

      Messages.BuildRequest.Builder build_request_pb_builder = Messages.BuildRequest.newBuilder();
      buildRequest.toProtocolBuffer(build_request_pb_builder);
      setResponseBody(build_request_pb_builder.build());
    }
  }

  @Override
  protected void put() throws RequestException {
    Messages.BuildRequest build_request_pb = getRequestBody(Messages.BuildRequest.class);
    try (Transaction t = DB.beginTransaction()) {

      // the only thing you can change is the notes
      new BuildQueueController(t).updateNotes(Integer.parseInt(build_request_pb.getKey()), build_request_pb.getNotes());

    } catch (Exception e) {
      throw new RequestException(e);
    }
  }
}
