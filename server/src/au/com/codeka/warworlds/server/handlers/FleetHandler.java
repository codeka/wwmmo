package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class FleetHandler extends RequestHandler {
    @Override
    protected void put() throws RequestException {
        Messages.Fleet fleet_pb = getRequestBody(Messages.Fleet.class);

        // make sure the fleet in the pb is the same as the one referenced in the URL
        if (!fleet_pb.getKey().equals(getUrlParameter("fleet_id"))) {
            throw new RequestException(400, "Invalid fleet_id");
        }
        if (!fleet_pb.getStarKey().equals(getUrlParameter("star_id"))) {
            throw new RequestException(400, "Invalid star_id");
        }

        // you're only allowed to update fleets that you own
        if (getSession().getEmpireID() != Integer.parseInt(fleet_pb.getEmpireKey())) {
            throw new RequestException(404, "No access to this fleet allowed.");
        }

        try (Transaction t = DB.beginTransaction()) {
            Simulation sim = new Simulation();
            Star star = new StarController(t).getStar(Integer.parseInt(getUrlParameter("star_id")));
            sim.simulate(star);

            int fleetID = Integer.parseInt(getUrlParameter("fleet_id"));
            int empireID = getSession().getEmpireID();
            for (BaseFleet baseFleet : star.getFleets()) {
                Fleet fleet = (Fleet) baseFleet;
                if (fleet.getID() == fleetID && fleet.getEmpireID() == empireID) {
                    fleet.setNotes(fleet_pb.getNotes());
                    new StarController(t).update(star);
                    break;
                }
            }

            t.commit();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }
}
