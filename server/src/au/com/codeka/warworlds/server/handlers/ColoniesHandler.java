package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ColonyController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.FleetController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Handles the /realms/.../stars/<star_id>/colonies URLs.
 */
public class ColoniesHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        Messages.ColonizeRequest colonize_request_pb = getRequestBody(Messages.ColonizeRequest.class);
        Empire myEmpire = new EmpireController().getEmpire(getSession().getEmpireID());

        try (Transaction t = DB.beginTransaction()) {
            // fetch the star, simulate & update it
            Star star = new StarController(t).getStar(Integer.parseInt(getUrlParameter("star_id")));
            Simulation sim = new Simulation();
            sim.simulate(star);
            new StarController(t).update(star);

            int planetIndex = colonize_request_pb.getPlanetIndex();

            // make sure there's no colony already on this planet
            for (BaseColony colony : star.getColonies()) {
                if (colony.getPlanetIndex() == planetIndex) {
                    throw new RequestException(400, Messages.GenericError.ErrorCode.CannotColonizePlanetAlreadyHasColony,
                                               "There is already a colony on this planet.");
                }
            }

            // find the fleet of colony ships we'll use to colonize this planet. Basically, we
            // choose an idle fleet with the smaller number of ships to take from.
            Fleet colonyShipFleet = null;
            for (BaseFleet baseFleet : star.getFleets()) {
                Fleet fleet = (Fleet) baseFleet;
                if (fleet.getEmpireID() != myEmpire.getID()) {
                    continue;
                }
                if (fleet.getState() != Fleet.State.IDLE) {
                    continue;
                }
                if (!fleet.getDesignID().equals("colonyship")) {
                    continue;
                }
                if (colonyShipFleet == null || colonyShipFleet.getNumShips() > fleet.getNumShips()) {
                    colonyShipFleet = fleet;
                }
            }

            if (colonyShipFleet == null) {
                throw new RequestException(400, Messages.GenericError.ErrorCode.CannotColonizePlanetNoColonyShips,
                                           "No idle Colony Ship is available to colonize this planet.");
            }

            // remove a ship from your colonyship fleet
            new FleetController(t).removeShips(star, colonyShipFleet, 1.0f);

            // and colonize the planet!
            Colony colony = new ColonyController(t).colonize(myEmpire, star, planetIndex);

            Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
            colony.toProtocolBuffer(colony_pb);
            setResponseBody(colony_pb.build());

            t.commit();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }
}
