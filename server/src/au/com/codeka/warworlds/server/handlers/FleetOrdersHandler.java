package au.com.codeka.warworlds.server.handlers;

import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.EventProcessor;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

public class FleetOrdersHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        Messages.FleetOrder fleet_order_pb = getRequestBody(Messages.FleetOrder.class);

        try (Transaction t = DB.beginTransaction()) {
            Simulation sim = new Simulation();
            Star star = new StarController(t).getStar(Integer.parseInt(getUrlParameter("star_id")));
            sim.simulate(star);

            int fleetID = Integer.parseInt(getUrlParameter("fleet_id"));
            int empireID = getSession().getEmpireID();
            for (BaseFleet baseFleet : star.getFleets()) {
                Fleet fleet = (Fleet) baseFleet;
                if (fleet.getID() == fleetID && fleet.getEmpireID() == empireID) {
                    boolean pingEventProcessor = orderFleet(star, fleet, fleet_order_pb, sim);
                    new StarController(t).update(star);
                    if (pingEventProcessor) {
                        EventProcessor.i.ping();
                    }
                    break;
                }
            }

            t.commit();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    private boolean orderFleet(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.SET_STANCE) {
            orderFleetSetStance(star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.SPLIT) {
            orderFleetSplit(star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.MERGE) {
            orderFleetMerge(star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.MOVE) {
            orderFleetMove(star, fleet, fleet_order_pb, sim);
            return true;
        }

        return false;
    }

    private void orderFleetSetStance(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) {
        fleet.setStance(BaseFleet.Stance.fromNumber(fleet_order_pb.getStance().getNumber()));

        // TODO: if we just set it to "aggressive" then assume we just arrived at the star
    }

    private void orderFleetSplit(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        if (fleet.getState() != Fleet.State.IDLE) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.CannotOrderFleetNotIdle,
                                       "Cannot split a fleet that is not currently idle.");
        }

        float totalShips = fleet.getNumShips();
        int leftShips = fleet_order_pb.getSplitLeft();
        float rightShips = totalShips - leftShips;
        if (rightShips < 1.0f || leftShips <= 0) {
            return; // can't split to less than 1
        }

        Fleet newFleet = fleet.split(rightShips);
        star.getFleets().add(newFleet);
    }

    private void orderFleetMerge(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        if (fleet.getState() != Fleet.State.IDLE) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.CannotOrderFleetNotIdle,
                                       "Cannot merge a fleet that is not currently idle.");
        }

        for (BaseFleet baseFleet : star.getFleets()) {
            if (baseFleet.getKey().equals(fleet_order_pb.getMergeFleetKey())) {
                Fleet otherFleet = (Fleet) baseFleet;
                if (otherFleet.getState() != Fleet.State.IDLE) {
                    throw new RequestException(400, Messages.GenericError.ErrorCode.CannotOrderFleetNotIdle,
                            "Cannot merge a fleet that is not currently idle.");
                }

                if (!otherFleet.getDesignID().equals(fleet.getDesignID())) {
                    throw new RequestException(400, Messages.GenericError.ErrorCode.CannotMergeFleetDifferentDesign,
                            "Cannot merge two fleets of a different design.");
                }

                fleet.setNumShips(fleet.getNumShips() + otherFleet.getNumShips());

                // TODO: probably not the best place for this to go...
                String sql = "DELETE FROM fleets WHERE id = ?";
                try (SqlStmt stmt = DB.prepare(sql)) {
                    stmt.setInt(1, otherFleet.getID());
                    stmt.update();
                } catch (Exception e) {
                    throw new RequestException(e);
                }
            }
        }
    }

    private void orderFleetMove(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        if (fleet.getState() != Fleet.State.IDLE) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.CannotOrderFleetNotIdle,
                                       "Cannot move a fleet that is not currently idle.");
        }

        Star srcStar = star;
        Star destStar = new StarController().getStar(Integer.parseInt(fleet_order_pb.getStarKey()));
        float distanceInParsecs = Sector.distanceInParsecs(srcStar, destStar);

        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
        float fuelCost = design.getFuelCost(distanceInParsecs, fleet.getNumShips());

        Messages.CashAuditRecord.Builder audit_record_pb = Messages.CashAuditRecord.newBuilder()
                .setEmpireId(getSession().getEmpireID())
                .setReason(Messages.CashAuditRecord.Reason.FleetMove)
                .setFleetDesignId(fleet.getDesignID())
                .setFleetId(fleet.getID())
                .setNumShips(fleet.getNumShips())
                .setStarId(destStar.getID())
                .setStarName(destStar.getName())
                .setMoveDistance(distanceInParsecs);

        if (!new EmpireController().withdrawCash(getSession().getEmpireID(), fuelCost, audit_record_pb)) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.InsufficientCash,
                                       "Insufficient cash for move.");
        }

        float moveTimeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();
        DateTime now = DateTime.now();
        fleet.move(now, destStar.getKey(), now.plusSeconds((int)(moveTimeInHours * 3600.0f)));
    }
}
