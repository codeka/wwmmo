package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.model.BaseFleet.State;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.EventProcessor;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.designeffects.EmptySpaceMoverShipEffect;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.FleetUpgrade;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

public class FleetOrdersHandler extends RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FleetOrdersHandler.class);

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
                    boolean pingEventProcessor = orderFleet(t, star, fleet, fleet_order_pb, sim);
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

    private boolean orderFleet(Transaction t, Star star, Fleet fleet,
                               Messages.FleetOrder fleet_order_pb,
                               Simulation sim) throws RequestException {
        if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.SET_STANCE) {
            orderFleetSetStance(star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.SPLIT) {
            orderFleetSplit(star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.MERGE) {
            orderFleetMerge(t, star, fleet, fleet_order_pb, sim);
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.MOVE) {
            orderFleetMove(star, fleet, fleet_order_pb, sim);
            return true;
        } else if (fleet_order_pb.getOrder() == Messages.FleetOrder.FLEET_ORDER.BOOST) {
            orderFleetBoost(star, fleet, fleet_order_pb, sim);
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

    private void orderFleetMerge(Transaction t, Star star, Fleet fleet,
                                 Messages.FleetOrder fleet_order_pb,
                                 Simulation sim) throws RequestException {
        if (fleet.getState() != Fleet.State.IDLE) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.CannotOrderFleetNotIdle,
                                       "Cannot merge a fleet that is not currently idle.");
        }

        ArrayList<BaseFleet> fleets = new ArrayList<BaseFleet>(star.getFleets());
        for (BaseFleet baseFleet : fleets) {
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

                String notes = fleet.getNotes();
                if (notes == null && otherFleet.getNotes() != null) {
                    notes = otherFleet.getNotes();
                } else if (otherFleet.getNotes() != null) {
                    notes += "\n" + otherFleet.getNotes();
                }
                fleet.setNumShips(fleet.getNumShips() + otherFleet.getNumShips());
                fleet.setNotes(notes);

                // only when the upgrade is in both fleets do we keep it.
                ArrayList<BaseFleetUpgrade> upgradesToRemove = new ArrayList<BaseFleetUpgrade>();
                for (BaseFleetUpgrade baseFleetUpgrade : fleet.getUpgrades()) {
                    log.info("baseFleetUpgrade = "+baseFleetUpgrade.getUpgradeID());
                    boolean keep = false;
                    for (BaseFleetUpgrade otherFleetUpgrade : otherFleet.getUpgrades()) {
                        log.info("otherFleetUpgrade = "+baseFleetUpgrade.getUpgradeID());
                        if (baseFleetUpgrade.getUpgradeID().equals(otherFleetUpgrade.getUpgradeID())) {
                            keep = true;
                        }
                    }

                    if (!keep) {
                        upgradesToRemove.add(baseFleetUpgrade);
                    }
                }
                fleet.getUpgrades().removeAll(upgradesToRemove);

                // TODO: probably not the best place for this to go...
                String sql = "DELETE FROM fleet_upgrades WHERE fleet_id = ?";
                try (SqlStmt stmt = t.prepare(sql)) {
                    stmt.setInt(1, otherFleet.getID());
                    stmt.update();
                } catch (Exception e) {
                    throw new RequestException(e);
                }

                sql = "DELETE FROM fleets WHERE id = ?";
                try (SqlStmt stmt = t.prepare(sql)) {
                    stmt.setInt(1, otherFleet.getID());
                    stmt.update();
                } catch (Exception e) {
                    throw new RequestException(e);
                }

                star.getFleets().remove(otherFleet);
            }
        }
    }

    private void orderFleetMove(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        if (fleet.getState() != Fleet.State.IDLE) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.CannotOrderFleetNotIdle,
                                       "Cannot move a fleet that is not currently idle.");
        }

        Star srcStar = star;
        Star destStar;
        if (fleet_order_pb.hasStarKey()) {
            destStar = orderFleetMoveStar(star, fleet, fleet_order_pb, sim);
        } else {
            destStar = orderFleetMoveSpace(star, fleet, fleet_order_pb, sim);
        }

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

    private Star orderFleetMoveStar(Star srcStar, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        if (fleet.getDesign().hasEffect(EmptySpaceMoverShipEffect.class)) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.FleetMoveCannotMoveToStar,
                    "This fleet cannot be moved to another star.");
        }

        Star destStar = new StarController().getStar(Integer.parseInt(fleet_order_pb.getStarKey()));

        // you can't move to a marker star either....
        if (destStar.getStarType().getInternalName().equals("marker")) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.FleetMoveCannotMoveToEmptySpace,
                    "This fleet can only be moved to stars.");
        }

        return destStar;
    }

    private Star orderFleetMoveSpace(Star srcStar, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        if (!fleet.getDesign().hasEffect(EmptySpaceMoverShipEffect.class)) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.FleetMoveCannotMoveToEmptySpace,
                    "This fleet can only be moved to stars.");
        }

        long sectorX = fleet_order_pb.getSectorX();
        long sectorY = fleet_order_pb.getSectorY();
        int offsetX = fleet_order_pb.getOffsetX();
        int offsetY = fleet_order_pb.getOffsetY();

        return new StarController().addMarkerStar(sectorX, sectorY, offsetX, offsetY);
    }

    private void orderFleetBoost(Star star, Fleet fleet, Messages.FleetOrder fleet_order_pb, Simulation sim) throws RequestException {
        FleetUpgrade.BoostFleetUpgrade boostFleetUpgrade = (FleetUpgrade.BoostFleetUpgrade) fleet.getUpgrade("boost");
        if (boostFleetUpgrade == null) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.FleetBoostNoUpgrade,
                    "This fleet does not have the 'Warp Boost' upgrade.");
        }

        if (fleet.getState() != State.MOVING) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.FleetBoostNotMoving, "Fleet is not moving.");
        }

        if (boostFleetUpgrade.isBoosting()) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.FleetBoostAlreadyBoosting, "Already boosting.");
        }

        boostFleetUpgrade.isBoosting(true);

        // halve the time remaining
        DateTime eta = fleet.getEta();
        int seconds = Seconds.secondsBetween(DateTime.now(), eta).getSeconds();
        seconds /= 2;
        fleet.setEta(DateTime.now().plusSeconds(seconds));
    }
}
