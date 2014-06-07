package au.com.codeka.warworlds.server.ctrl;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BaseScoutReport;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.BuildingEffect;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.EventProcessor;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.designeffects.RadarBuildingEffect;
import au.com.codeka.warworlds.server.model.Alliance;
import au.com.codeka.warworlds.server.model.BuildRequest;
import au.com.codeka.warworlds.server.model.Building;
import au.com.codeka.warworlds.server.model.BuildingPosition;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.CombatReport;
import au.com.codeka.warworlds.server.model.EmpirePresence;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.FleetUpgrade;
import au.com.codeka.warworlds.server.model.Planet;
import au.com.codeka.warworlds.server.model.ScoutReport;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

public class StarController {
    private static final Logger log = LoggerFactory.getLogger(StarController.class);
    private DataBase db;

    public StarController() {
        db = new DataBase();
    }
    public StarController(Transaction trans) {
        db = new DataBase(trans);
    }

    public Star getStar(int id) throws RequestException {
        List<Star> stars = db.getStars(new int[] {id});
        if (stars.isEmpty()) {
            throw new RequestException(404);
        }
        return stars.get(0);
    }

    public List<Star> getStars(int[] ids) throws RequestException {
        return db.getStars(ids);
    }

    public List<Star> getStars(Collection<Integer> ids) throws RequestException {
        int[] idArray = new int[ids.size()];
        int i = 0;
        for (Integer id : ids) {
            idArray[i++] = id;
        }
        return db.getStars(idArray);
    }

    public List<Star> getWormholesForAlliance(int allianceID) throws RequestException {
        Alliance alliance = new AllianceController().getAlliance(allianceID);
        try {
            return db.getWormholesForAlliance(alliance);
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    public Star addMarkerStar(long sectorX, long sectorY, int offsetX, int offsetY) throws RequestException {
        // TODO: check that this isn't too close to an existing star...

        Sector sector = new SectorController().getSector(sectorX, sectorY);
        try {
            int starID = db.addStar(sector.getID(), offsetX, offsetY, 20, "Marker", Star.Type.Marker, null);
            return getStar(starID);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public void update(Star star) throws RequestException {
        try {
            updateNoRetry(star);
        } catch (Exception e) {
            throw new RequestException(e);
        }

        // we may need to ping the event processor if a build time change, or whatever.
        EventProcessor.i.ping();
    }

    private void updateNoRetry(Star star) throws Exception {
        db.updateStar(star);
        removeEmpirePresences(star.getID());
    }

    public void removeEmpirePresences(int starID) throws RequestException {
        // delete an empire presences for empires that no longer have colonies on this star...
        String sql = "DELETE FROM empire_presences" +
                     " WHERE star_id = ?" +
                     " AND (SELECT COUNT(*)" +
                          " FROM colonies" +
                          " WHERE colonies.empire_id = empire_presences.empire_id" +
                          " AND colonies.star_id = empire_presences.star_id) = 0";
        try (SqlStmt stmt = db.prepare(sql)) {
            stmt.setInt(1, starID);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    /**
     * "Sanitizes" a star and removes all info specific to other empires.
     * @param star
     * @param myEmpireID
     */
    public void sanitizeStar(Star star, int myEmpireID,
                             ArrayList<BuildingPosition> buildings,
                             ArrayList<Star> otherStars) {
        log.debug(String.format("Sanitizing star %s (%d, %d) (%d, %d)",
                star.getName(), star.getSectorX(), star.getSectorY(), star.getOffsetX(), star.getOffsetY()));

        // if the star is a wormhole, don't sanitize it -- a wormhole is basically fleets in
        // transit anyway
        if (star.getStarType().getType() == Star.Type.Wormhole) {
            return;
        }

        // if we don't have any fleets here, remove all the others
        boolean removeFleets = true;
        ArrayList<Fleet> fleetsToAddBack = null;
        for (BaseFleet baseFleet : star.getFleets()) {
            Fleet fleet = (Fleet) baseFleet;
            if (fleet.getEmpireID() != null && fleet.getEmpireID() == myEmpireID) {
                removeFleets = false;
            }
        }
        // ... unless we have a radar on a nearby star
        if (buildings != null) for (BuildingPosition building : buildings) {
            BuildingDesign design = building.getDesign();
            float radarRange = 0.0f;
            for (RadarBuildingEffect effect : design.getEffects(building.getLevel(), RadarBuildingEffect.class)) {
                if (effect.getRange() > radarRange) {
                    radarRange = effect.getRange();
                }
            }

            if (radarRange > 0.0f) {
                log.debug(String.format("Building position: (%d, %d) (%d, %d)",
                        building.getSectorX(), building.getSectorY(), building.getOffsetX(), building.getOffsetY()));
                float distanceToBuilding = Sector.distanceInParsecs(star,
                        building.getSectorX(), building.getSectorY(),
                        building.getOffsetX(), building.getOffsetY());
                if (distanceToBuilding < radarRange) {
                    log.debug(String.format("Distance to building (%.2f) > radar range (%.2f), keeping fleets.",
                            distanceToBuilding, radarRange));
                    removeFleets = false;
                }

                if (removeFleets && otherStars != null) {
                    // check any moving fleets, we'll want to add those back
                    for (BaseFleet baseFleet : star.getFleets()) {
                        if (baseFleet.getState() != Fleet.State.MOVING) {
                            continue;
                        }
                        Fleet fleet = (Fleet) baseFleet;

                        Star destinationStar = null;
                        for (Star otherStar : otherStars) {
                            if (otherStar.getID() == fleet.getDestinationStarID()) {
                                destinationStar = otherStar;
                                break;
                            }
                        }
                        if (destinationStar != null) {
                            Vector2 dir = Sector.directionBetween(star, destinationStar);
                            float progress = fleet.getMovementProgress();
                            log.debug(String.format("Fleet's distance to destination: %.2f, progress=%.2f", dir.length(), progress));
                            dir.scale(progress);

                            float distanceToFleet = Sector.distanceInParsecs(
                                    star.getSectorX(), star.getSectorY(),
                                    star.getOffsetX() + (int) (dir.x * Sector.PIXELS_PER_PARSEC),
                                    star.getOffsetY() + (int) (dir.y * Sector.PIXELS_PER_PARSEC),
                                    building.getSectorX(), building.getSectorY(),
                                    building.getOffsetX(), building.getOffsetY());
                            if (distanceToFleet < radarRange) {
                                if (fleetsToAddBack == null) {
                                    fleetsToAddBack = new ArrayList<Fleet>();
                                }
                                log.debug(String.format("Adding fleet %d (%s x %.2f) back.",
                                        fleet.getID(), fleet.getDesignID(), fleet.getNumShips()));
                                fleetsToAddBack.add(fleet);
                            } else {
                                log.debug(String.format("distance to fleet (%.2f) >= radar range (%.2f)", distanceToFleet, radarRange));
                            }
                        }
                    }
                }
            }
        }
        if (removeFleets) {
            star.getFleets().clear();
            if (fleetsToAddBack != null) {
                star.getFleets().addAll(fleetsToAddBack);
            }
        }

        // remove all fleets that aren't ours and have a cloaking device (regardless of radars)
        ArrayList<Fleet> fleetsToRemove = null;
        for (BaseFleet baseFleet : star.getFleets()) {
            Fleet fleet = (Fleet) baseFleet;
            if (fleet.hasUpgrade("cloak") && fleet.getEmpireID() != myEmpireID) {
                if (fleetsToRemove == null) {
                    fleetsToRemove = new ArrayList<Fleet>();
                }
                fleetsToRemove.add(fleet);
            }
        }
        if (fleetsToRemove != null) {
            star.getFleets().removeAll(fleetsToRemove);
        }

        // remove build requests that aren't ours
        if (star.getBuildRequests() != null) {
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
        if (star.getScoutReports() != null) {
            ArrayList<BaseScoutReport> toRemove = new ArrayList<BaseScoutReport>();
            for (BaseScoutReport baseScoutReport : star.getScoutReports()) {
                ScoutReport scoutReport = (ScoutReport) baseScoutReport;
                if (!scoutReport.getEmpireKey().equals(Integer.toString(myEmpireID))) {
                    toRemove.add(baseScoutReport);
                }
            }
            star.getScoutReports().removeAll(toRemove);
        }

        // for any colonies that are not ours, hide some "secret" information
        for (BaseColony baseColony : star.getColonies()) {
            Colony colony = (Colony) baseColony;
            if (colony.getEmpireID() != null && colony.getEmpireID() != myEmpireID) {
                colony.sanitize();
            }
        }
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public List<Star> getStars(int[] ids) throws RequestException {
            if (ids.length == 0) {
                return new ArrayList<Star>();
            }

            ArrayList<Star> stars = new ArrayList<Star>();
            final String sql = "SELECT stars.id, sector_id, name, sectors.x AS sector_x," +
                                     " sectors.y AS sector_y, stars.x, stars.y, size, star_type, planets," +
                                     " extra, last_simulation, time_emptied" +
                              " FROM stars" +
                              " INNER JOIN sectors ON stars.sector_id = sectors.id" +
                              " WHERE stars.id IN "+buildInClause(ids);
            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                while (res.next()) {
                    stars.add(new Star(res));
                }

                if (stars.isEmpty()) {
                    return stars;
                }
            } catch(Exception e) {
                throw new RequestException(e);
            }

            int[] starIds = new int[stars.size()];
            for (int i = 0; i < stars.size(); i++) {
                Star star = stars.get(i);
                star.setColonies(new ArrayList<BaseColony>());
                star.setFleets(new ArrayList<BaseFleet>());
                star.setEmpires(new ArrayList<BaseEmpirePresence>());
                star.setBuildRequests(new ArrayList<BaseBuildRequest>());
                starIds[i] = star.getID();
            }

            String inClause = buildInClause(starIds);
            try {
                populateEmpires(stars, inClause);
                populateColonies(stars, inClause);
                populateFleets(stars, inClause);
                populateBuildings(stars, inClause);
                populateBuildRequests(stars, inClause);
                checkNativeColonies(stars);
                populateCombatReports(stars, inClause);
            } catch(Exception e) {
                throw new RequestException(e);
            }
            return stars;
        }

        public List<Star> getWormholesForAlliance(Alliance alliance) throws Exception {
            String sql = "SELECT stars.id, sector_id, name, sectors.x AS sector_x," +
                               " sectors.y AS sector_y, stars.x, stars.y, size, star_type, planets," +
                               " extra, last_simulation, time_emptied" +
                        " FROM stars" +
                        " INNER JOIN sectors ON stars.sector_id = sectors.id" +
                        " WHERE star_type = "+Star.Type.Wormhole.ordinal();
            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                ArrayList<Star> stars = new ArrayList<Star>();
                while (res.next()) {
                    Star star = new Star(res);
                    if (star.getWormholeExtra() == null) {
                        continue;
                    }
                    int empireID = star.getWormholeExtra().getEmpireID();
                    if (alliance.isEmpireMember(empireID)) {
                        stars.add(star);
                    }
                }

                return stars;
            }
        }

        public int addStar(int sectorID, int x, int y, int size, String name, Star.Type starType, Planet[] planets) throws Exception {
            String sql = "INSERT INTO stars (sector_id, x, y, size, name, star_type, planets, last_simulation, time_emptied)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            try (SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, sectorID);
                stmt.setInt(2, x);
                stmt.setInt(3, y);
                stmt.setInt(4, size);
                stmt.setString(5, name);
                stmt.setInt(6, starType.ordinal());

                Messages.Planets.Builder planets_pb = Messages.Planets.newBuilder();
                if (planets == null) {
                } else {
                    for (Planet planet : planets) {
                        Messages.Planet.Builder planet_pb = Messages.Planet.newBuilder();
                        planet.toProtocolBuffer(planet_pb);
                        planets_pb.addPlanets(planet_pb);
                    }
                }
                stmt.setBytes(7, planets_pb.build().toByteArray());

                stmt.update();
                return stmt.getAutoGeneratedID();
            }
        }

        public void updateStar(Star star) throws Exception {
            final String sql = "UPDATE stars SET" +
                                 " last_simulation = ?," +
                                 " name = ?," +
                                 " star_type = ?," +
                                 " empire_count = ?," +
                                 " extra = ?" +
                              " WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                DateTime lastSimulation = star.getLastSimulation();
                DateTime now = DateTime.now();
                if (lastSimulation.isAfter(now)) {
                    int difference = Seconds.secondsBetween(now, lastSimulation).getSeconds();
                    if (difference > 120) {
                        log.error(String.format(Locale.ENGLISH,
                                    "last_simulation is after now! [last_simulation=%s] [now=%s] [difference=%d seconds]",
                                    lastSimulation, now, difference),
                                  new Throwable());
                        lastSimulation = DateTime.now();
                    }
                }
                stmt.setDateTime(1, lastSimulation);
                stmt.setString(2, star.getName());
                stmt.setInt(3, star.getStarType().getType().ordinal());

                int empireCount = 0;
                for (BaseEmpirePresence empirePresence : star.getEmpirePresences()) {
                    if (empirePresence.getEmpireKey() != null) {
                        empireCount ++;
                    }
                }
                stmt.setInt(4, empireCount);

                Messages.Star.StarExtra.Builder star_extra_pb = null;
                if (star.getWormholeExtra() != null) {
                    if (star_extra_pb == null) {
                        star_extra_pb = Messages.Star.StarExtra.newBuilder();
                    }
                    star.getWormholeExtra().toProtocolBuffer(star_extra_pb);
                }
                if (star_extra_pb == null) {
                    stmt.setNull(5);
                } else {
                    stmt.setBytes(5, star_extra_pb.build().toByteArray());
                }

                stmt.setInt(6, star.getID());
                stmt.update();
            }

            updateEmpires(star);
            updateColonies(star);
            updateFleets(star);
            updateFleetUpgrades(star);
            updateBuildRequests(star);
            CombatReport combatReport = (CombatReport) star.getCombatReport();
            if (combatReport != null) {
                updateCombatReport(star, combatReport);
            }
        }

        private void updateEmpires(Star star) throws Exception {
            final String sql = "UPDATE empire_presences SET" +
                                 " total_goods = ?," +
                                 " total_minerals = ?," +
                                 " tax_per_hour = ?," +
                                 " goods_zero_time = ?" +
                              " WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                for (BaseEmpirePresence empire : star.getEmpires()) {
                    stmt.setDouble(1, empire.getTotalGoods());
                    stmt.setDouble(2, empire.getTotalMinerals());
                    stmt.setDouble(3, empire.getTaxPerHour());
                    stmt.setDateTime(4, empire.getGoodsZeroTime());
                    stmt.setInt(5, ((EmpirePresence) empire).getID());
                    stmt.update();
                }
            }
        }

        private void updateColonies(Star star) throws Exception {
            boolean needDelete = false;

            final float MIN_POPULATION = 0.0001f;

            TreeMap<Integer, Float> empireTaxes = new TreeMap<Integer, Float>();

            String sql = "UPDATE colonies SET" +
                           " focus_population = ?," +
                           " focus_construction = ?," +
                           " focus_farming = ?," +
                           " focus_mining = ?," +
                           " population = ?," +
                           " uncollected_taxes = ?" +
                        " WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                for (BaseColony baseColony : star.getColonies()) {
                    Colony colony = (Colony) baseColony;
                    if (colony.getPopulation() <= MIN_POPULATION) {
                        needDelete = true;
                        continue;
                    }

                    if (colony.getEmpireID() != null) {
                        Float uncollectedTaxes = empireTaxes.get(colony.getEmpireID());
                        uncollectedTaxes = (uncollectedTaxes == null ? 0 : uncollectedTaxes) +
                                colony.getUncollectedTaxes();
                        empireTaxes.put(colony.getEmpireID(), uncollectedTaxes);
                    }

                    stmt.setDouble(1, colony.getPopulationFocus());
                    stmt.setDouble(2, colony.getConstructionFocus());
                    stmt.setDouble(3, colony.getFarmingFocus());
                    stmt.setDouble(4, colony.getMiningFocus());
                    stmt.setDouble(5, colony.getPopulation());
                    stmt.setDouble(6, 0); // TODO: remove this column from the database
                    stmt.setInt(7, ((Colony) colony).getID());
                    stmt.update();

                    colony.setUncollectedTaxes(0.0f);
                }
            }

            if (!empireTaxes.isEmpty()) {
                sql = "UPDATE empires SET cash = cash + ? WHERE id = ? RETURNING cash";
                try (SqlStmt stmt = prepare(sql)) {
                    for (Map.Entry<Integer, Float> entry : empireTaxes.entrySet()) {
                        stmt.setDouble(1, entry.getValue());
                        stmt.setInt(2, entry.getKey());
                        SqlResult res = stmt.updateAndSelect();
                        if (res.next()) {
                            double totalCash = res.getDouble(1);

                            // send a notification that cash has been updated
                            new NotificationController().sendNotificationToOnlineEmpire(entry.getKey(),
                                    "cash", Double.toString(totalCash));
                        }
                    }
                }
            }

            if (needDelete) {
                ArrayList<BaseColony> toRemove = new ArrayList<BaseColony>();
                sql = "DELETE FROM colonies WHERE id = ?";
                try (SqlStmt stmt = prepare(sql)) {
                    for (BaseColony colony : star.getColonies()) {
                        if (colony.getPopulation() > MIN_POPULATION) {
                            continue;
                        }
                        stmt.setInt(1, ((Colony) colony).getID());
                        stmt.update();
                        toRemove.add(colony);
                    }

                    star.getColonies().removeAll(toRemove);
                }
            }
        }

        private void updateFleets(Star star) throws Exception {
            boolean needInsert = false;
            boolean needDelete = false;
            DateTime now = DateTime.now();
            String sql = "UPDATE fleets SET" +
                            " star_id = ?," +
                            " sector_id = ?," +
                            " num_ships = ?," +
                            " stance = ?," +
                            " state = ?," +
                            " state_start_time = ?," +
                            " eta = ?," +
                            " target_star_id = ?," +
                            " target_fleet_id = ?," +
                            " time_destroyed = ?," +
                            " notes = ?" +
                        " WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                for (BaseFleet baseFleet : star.getFleets()) {
                    if (baseFleet.getKey() == null) {
                        needInsert = true;
                        continue;
                    }
                    if (baseFleet.getTimeDestroyed() != null && baseFleet.getTimeDestroyed().isBefore(now)) {
                        needDelete = true;
                        continue;
                    }

                    Fleet fleet = (Fleet) baseFleet;
                    stmt.setInt(1, star.getID());
                    stmt.setInt(2, star.getSectorID());
                    stmt.setDouble(3, fleet.getNumShips());
                    stmt.setInt(4, fleet.getStance().getValue());
                    stmt.setInt(5, fleet.getState().getValue());
                    stmt.setDateTime(6, fleet.getStateStartTime());
                    stmt.setDateTime(7, fleet.getEta());
                    stmt.setInt(8, fleet.getDestinationStarID());
                    stmt.setInt(9, fleet.getTargetFleetID());
                    stmt.setDateTime(10, fleet.getTimeDestroyed());
                    stmt.setString(11, fleet.getNotes());
                    stmt.setInt(12, fleet.getID());
                    stmt.update();
                }
            }

            if (needInsert) {
                sql = "INSERT INTO fleets (star_id, sector_id, design_id, empire_id, num_ships," +
                                         " stance, state, state_start_time, eta, target_star_id," +
                                         " target_fleet_id, time_destroyed, notes)" +
                     " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                    for (BaseFleet baseFleet : star.getFleets()) {
                        if (baseFleet.getKey() != null) {
                            continue;
                        }

                        Fleet fleet = (Fleet) baseFleet;
                        stmt.setInt(1, fleet.getStarID());
                        stmt.setInt(2, fleet.getSectorID());
                        stmt.setString(3, fleet.getDesignID());
                        if (fleet.getEmpireKey() != null) {
                            stmt.setInt(4, fleet.getEmpireID());
                        } else {
                            stmt.setNull(4);
                        }
                        stmt.setDouble(5, fleet.getNumShips());
                        stmt.setInt(6, fleet.getStance().getValue());
                        stmt.setInt(7, fleet.getState().getValue());
                        stmt.setDateTime(8, fleet.getStateStartTime());
                        stmt.setDateTime(9, fleet.getEta());
                        stmt.setInt(10, fleet.getDestinationStarID());
                        stmt.setInt(11, fleet.getTargetFleetID());
                        stmt.setDateTime(12, fleet.getTimeDestroyed());
                        stmt.setString(13, fleet.getNotes());
                        stmt.update();
                        fleet.setID(stmt.getAutoGeneratedID());
                    }
                }
            }

            if (needDelete) {
                sql = "DELETE FROM fleet_upgrades WHERE fleet_id = ?";
                try (SqlStmt stmt = prepare(sql)) {
                    for (BaseFleet baseFleet : star.getFleets()) {
                        if (baseFleet.getTimeDestroyed() != null && baseFleet.getTimeDestroyed().isBefore(now)) {
                            Fleet fleet = (Fleet) baseFleet;
                            stmt.setInt(1, fleet.getID());
                            stmt.update();
                        }
                    }
                } catch(Exception e) {
                    throw new RequestException(e);
                }

                sql = "DELETE FROM fleets WHERE id = ?";
                ArrayList<BaseFleet> toRemove = new ArrayList<BaseFleet>();
                try (SqlStmt stmt = prepare(sql)) {
                    for (BaseFleet baseFleet : star.getFleets()) {
                        if (baseFleet.getTimeDestroyed() != null && baseFleet.getTimeDestroyed().isBefore(now)) {
                            Fleet fleet = (Fleet) baseFleet;
                            stmt.setInt(1, fleet.getID());
                            stmt.update();
                            toRemove.add(baseFleet);
                        }
                    }

                    star.getFleets().removeAll(toRemove);
                } catch(Exception e) {
                    throw new RequestException(e);
                }
            }
        }

        private void updateFleetUpgrades(Star star) throws Exception {
            String sql = "DELETE FROM fleet_upgrades WHERE star_id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, star.getID());
                stmt.update();
            }

            DateTime now = DateTime.now();
            sql = "INSERT INTO fleet_upgrades (star_id, fleet_id, upgrade_id, extra) VALUES (?, ?, ?, ?)";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, star.getID());
                for (BaseFleet baseFleet : star.getFleets()) {
                    if (baseFleet.getUpgrades() == null || baseFleet.getUpgrades().isEmpty()) {
                        continue;
                    }
                    if (baseFleet.getTimeDestroyed() != null && baseFleet.getTimeDestroyed().isBefore(now)) {
                        continue;
                    }

                    Fleet fleet = (Fleet) baseFleet;
                    stmt.setInt(2, fleet.getID());
                    for (BaseFleetUpgrade upgrade : fleet.getUpgrades()) {
                        stmt.setString(3, upgrade.getUpgradeID());
                        stmt.setString(4, upgrade.getExtra());
                        stmt.update();
                    }
                }
            }
        }

        private void updateBuildRequests(Star star) throws Exception {
            String sql = "UPDATE build_requests SET progress = ?, end_time = ?, disable_notification = ? WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                for (BaseBuildRequest baseBuildRequest : star.getBuildRequests()) {
                    BuildRequest buildRequest = (BuildRequest) baseBuildRequest;
                    stmt.setDouble(1, buildRequest.getProgress(false));
                    stmt.setDateTime(2, buildRequest.getEndTime());
                    stmt.setInt(3, buildRequest.getDisableNotification() ? 1 : 0);
                    stmt.setInt(4, buildRequest.getID());
                    stmt.update();
                }
            }
        }

        private void updateCombatReport(Star star, CombatReport combatReport) throws Exception {
            Messages.CombatReport.Builder pb = Messages.CombatReport.newBuilder();
            combatReport.toProtocolBuffer(pb);

            String sql;
            if (combatReport.getKey() == null) {
                sql = "INSERT INTO combat_reports (star_id, start_time, end_time, rounds) VALUES (?, ?, ?, ?)";
            } else {
                sql = "UPDATE combat_reports SET star_id = ?, start_time = ?, end_time = ?, rounds = ? WHERE id = ?";
            }
            try (SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, star.getID());
                stmt.setDateTime(2, combatReport.getStartTime());
                stmt.setDateTime(3, combatReport.getEndTime());
                stmt.setBytes(4, pb.build().toByteArray());
                if (combatReport.getKey() != null) {
                    stmt.setInt(5, Integer.parseInt(combatReport.getKey()));
                }
                stmt.update();
                combatReport.setID(stmt.getAutoGeneratedID());
            }
        }

        private void populateColonies(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT * FROM colonies WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                while (res.next()) {
                    Colony colony = new Colony(res);

                    for (Star star : stars) {
                        if (star.getID() == colony.getStarID()) {
                            // max population for the colony is initially just it's congeniality
                            BasePlanet planet = star.getPlanets()[colony.getPlanetIndex() - 1];
                            colony.setMaxPopulation(planet.getPopulationCongeniality());

                            star.getColonies().add(colony);
                        }
                    }
                }
            }
        }

        private void populateFleets(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT fleets.*, empires.alliance_id" +
                        " FROM fleets" +
                        " LEFT OUTER JOIN empires ON empires.id = fleets.empire_id" +
                        " WHERE star_id IN "+inClause;

            ArrayList<Fleet> fleets = new ArrayList<Fleet>();
            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                while (res.next()) {
                    Fleet fleet = new Fleet(res);
                    fleets.add(fleet);

                    for (Star star : stars) {
                        if (star.getID() == fleet.getStarID()) {
                            star.getFleets().add(fleet);
                        }
                    }
                }
            }

            sql = "SELECT * FROM fleet_upgrades WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                while (res.next()) {
                    FleetUpgrade fleetUpgrade = FleetUpgrade.create(res);

                    for (Fleet fleet : fleets) {
                        if (fleet.getID() == fleetUpgrade.getFleetID()) {
                            fleet.getUpgrades().add(fleetUpgrade);
                            break;
                        }
                    }
                }
            }
        }

        private void populateEmpires(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT * FROM empire_presences WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                while (res.next()) {
                    EmpirePresence empirePresence = new EmpirePresence(res);

                    for (Star star : stars) {
                        if (star.getID() == empirePresence.getStarID()) {
                            // by default, you get 500 max goods/minerals
                            empirePresence.setMaxGoods(500);
                            empirePresence.setMaxMinerals(500);

                            star.getEmpirePresences().add(empirePresence);
                        }
                    }
                }
            }
        }

        private void populateBuildRequests(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT * FROM build_requests WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                while (res.next()) {
                    int starID = res.getInt("star_id");
                    Star star = null;
                    for (Star thisStar : stars) {
                        if (thisStar.getID() == starID) {
                            star = thisStar;
                            break;
                        }
                    }

                    BuildRequest buildRequest = new BuildRequest(star, res);
                    star.getBuildRequests().add(buildRequest);
                }
            }
        }

        private void populateBuildings(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT * FROM buildings WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                while (res.next()) {
                    Building building = new Building(res);

                    for (Star star : stars) {
                        for (BaseColony baseColony : star.getColonies()) {
                            Colony colony = (Colony) baseColony;
                            if (colony.getID() == building.getColonyID()) {
                                for (Design.Effect effect : building.getDesign().getEffects(building.getLevel())) {
                                    BuildingEffect buildingEffect = (BuildingEffect) effect;
                                    buildingEffect.apply(star, colony, building);
                                }
                                colony.getBuildings().add(building);
                            }
                        }
                    }
                }
            }
        }

        private void populateCombatReports(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT star_id, rounds FROM combat_reports WHERE star_id IN "+inClause;
            sql += " AND end_time > ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setDateTime(1, DateTime.now());
                SqlResult res = stmt.select();

                while (res.next()) {
                    int starID = res.getInt(1);
                    Messages.CombatReport pb = Messages.CombatReport.parseFrom(res.getBytes(2));
                    CombatReport combatReport = new CombatReport();
                    combatReport.fromProtocolBuffer(pb);

                    for (Star star : stars) {
                        if (star.getID() == starID) {
                            star.setCombatReport(combatReport);
                        }
                    }
                }
            }
            
        }

        /**
         * Checks if any of the stars in the given list need native colonies added, and adds them
         * if so.
         */
        private void checkNativeColonies(List<Star> stars) throws Exception {
            for (Star star : stars) {
                // marker and wormhole don't get colonies anyway
                if (star.getStarType().getType() == Star.Type.Marker ||
                        star.getStarType().getType() == Star.Type.Wormhole) {
                    continue;
                }

                // first, make sure there's no colonies and no fleets
                if (!star.getColonies().isEmpty() || !star.getFleets().isEmpty()) {
                    continue;
                }

                // next, if it was only emptied 3 days ago, don't add more just yet
                if (star.getTimeEmptied() != null && star.getTimeEmptied().isAfter(DateTime.now().minusDays(3))) {
                    continue;
                }

                // OK, add those native colonies!
                addNativeColonies(star);
            }
        }

        private void addNativeColonies(Star star) throws Exception {
            ArrayList<Planet> planets = new ArrayList<Planet>();
            for (int i = 0; i < star.getPlanets().length; i++) {
                planets.add((Planet) star.getPlanets()[i]);
            }

            // sort the planets in order of most desirable to least desirable
            Collections.sort(planets, new Comparator<Planet>() {
                @Override
                public int compare(Planet lhs, Planet rhs) {
                    double lhsScore = lhs.getPopulationCongeniality() +
                                      (lhs.getFarmingCongeniality() * 0.75) +
                                      (lhs.getMiningCongeniality() * 0.5);
                    double rhsScore = rhs.getPopulationCongeniality() +
                                      (rhs.getFarmingCongeniality() * 0.75) +
                                      (rhs.getMiningCongeniality() * 0.5);
                    return Double.compare(rhsScore, lhsScore);
                }
            });

            Random rand = new Random();
            int numColonies = rand.nextInt(Math.min(3, planets.size() - 1)) + 1;
            for (int i = 0; i < numColonies; i++) {
                Planet planet = planets.get(i);
                Colony colony = new ColonyController(getTransaction()).colonize(null, star, planet.getIndex(), 100.0f);
                colony.setConstructionFocus(0.0f);
                colony.setPopulationFocus(0.5f);
                colony.setFarmingFocus(0.5f);
                colony.setMiningFocus(0.0f);
                colony.setMaxPopulation(planet.getPopulationCongeniality());
            }

            int numFleets = rand.nextInt(4) + 1;
            for (int i = 0; i < numFleets; i++) {
                float numShips = (rand.nextInt(5) + 1) * 5;
                new FleetController(getTransaction()).createFleet(null, star, "fighter", numShips);
            }

            // simulate for 24 hours to make it look like it's being doing stuff before you got here...
            star.setLastSimulation(DateTime.now().minusHours(24));
            Simulation sim = new Simulation();
            sim.simulate(star);

            updateStar(star);
        }
    }
}
