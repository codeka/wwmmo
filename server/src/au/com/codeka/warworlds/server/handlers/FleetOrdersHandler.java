package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class FleetOrdersHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        Messages.FleetOrder fleet_order_pb = getRequestBody(Messages.FleetOrder.class);

        Simulation sim = new Simulation();
        Star star = new StarController().getStar(Integer.parseInt(getUrlParameter("star_id")));
        sim.simulate(star);

        int fleetID = Integer.parseInt(getUrlParameter("fleet_id"));
        int empireID = getSession().getEmpireID();
        for (BaseFleet baseFleet : star.getFleets()) {
            Fleet fleet = (Fleet) baseFleet;
            if (fleet.getID() == fleetID && fleet.getEmpireID() == empireID) {
                orderFleet(star, fleet, fleet_order_pb, sim);
                new StarController().update(star);
                break;
            }
        }
    }

    private void orderFleet(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.SET_STANCE) {
            orderFleetSetStance(star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.SPLIT) {
            orderFleetSplit(star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.MERGE) {
            orderFleetMerge(star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.MOVE) {
            orderFleetMove(star, fleet, fleet_order_pb, sim);
        }
    }

    private void orderFleetSetStance(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) {
        fleet.setStance(BaseFleet.Stance.fromNumber(fleet_order_pb.getStance().getNumber()));

        // TODO: if we just set it to "aggressive" then assume we just arrived at the star
    }

    private void orderFleetSplit(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        float totalShips = fleet.getNumShips();
        int leftShips = fleet_order_pb.getSplitLeft();
        float rightShips = totalShips - leftShips;
        if (rightShips < 1.0f || leftShips <= 0) {
            return; // can't split to less than 1
        }

        if (fleet.getState() != Fleet.State.IDLE) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.CannotOrderFleetNotIdle,
                                       "Cannot split a fleet that is not currently idle.");
        }

        Fleet newFleet = fleet.split(rightShips);
        star.getFleets().add(newFleet);
    }

    private void orderFleetMerge(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) {
        
    }

    private void orderFleetMove(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) {
        
    }
}
