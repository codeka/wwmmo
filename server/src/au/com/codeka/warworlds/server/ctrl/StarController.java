package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BuildingEffect;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.EventProcessor;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.BuildRequest;
import au.com.codeka.warworlds.server.model.Building;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.CombatReport;
import au.com.codeka.warworlds.server.model.EmpirePresence;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Planet;
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

    public void update(Star star) throws RequestException {
        db.updateStar(star);

        // we may need to ping the event processor if a build time change, or whatever.
        EventProcessor.i.ping();
    }

    public void simulateAllStarsOlderThan(DateTime dt) {
        // this'll be a number, 0..6. We want to try & spread out the load throughout the whole
        // day, and this will ensure an individual star is only elligible once every 6 hours
        int mod = dt.getHourOfDay() / 4;
        while (true) {
            ArrayList<Integer> starIDs = new ArrayList<Integer>();
            String sql = "SELECT id FROM stars WHERE last_simulation < ? AND" +
                        " (SELECT COUNT(*) FROM colonies WHERE star_id = stars.id) > 0" +
                        " AND (id % 6 = "+mod+")" +
                        " LIMIT 25";
            try (SqlStmt stmt = db.prepare(sql)) {
                stmt.setDateTime(1, dt);
                ResultSet rs = stmt.select();
                while (rs.next()) {
                    starIDs.add(rs.getInt(1));
                }
            } catch (Exception e) {
            }

            if (starIDs.size() == 0) {
                break;
            }

            int[] ids = new int[starIDs.size()];
            for (int i = 0; i < starIDs.size(); i++) {
                ids[i] = starIDs.get(i);
            }

            try {
                Simulation sim = new Simulation();
                for (Star star : getStars(ids)) {
                    sim.simulate(star);
                    update(star);
                }
            } catch(RequestException e) {
                log.error("Unhandled exception simulating star", e);
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
                                     " last_simulation, time_emptied" +
                              " FROM stars" +
                              " INNER JOIN sectors ON stars.sector_id = sectors.id" +
                              " WHERE stars.id IN "+buildInClause(ids);
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    stars.add(new Star(rs));
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

        public void updateStar(Star star) throws RequestException {
            final String sql = "UPDATE stars SET" +
                                 " last_simulation = ?" +
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
                stmt.setInt(2, star.getID());
                stmt.update();
            } catch(Exception e) {
                throw new RequestException(e);
            }

            updateEmpires(star);
            updateColonies(star);
            updateFleets(star);
            updateBuildRequests(star);
            CombatReport combatReport = (CombatReport) star.getCombatReport();
            if (combatReport != null) {
                updateCombatReport(star, combatReport);
            }
        }

        private void updateEmpires(Star star) throws RequestException {
            final String sql = "UPDATE empire_presences SET" +
                                 " total_goods = ?," +
                                 " total_minerals = ?," +
                                 " goods_zero_time = ?" +
                              " WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                for (BaseEmpirePresence empire : star.getEmpires()) {
                    stmt.setDouble(1, empire.getTotalGoods());
                    stmt.setDouble(2, empire.getTotalMinerals());
                    stmt.setDateTime(3, empire.getGoodsZeroTime());
                    stmt.setInt(4, ((EmpirePresence) empire).getID());
                    stmt.update();
                }
            } catch(Exception e) {
               throw new RequestException(e);
            }
        }

        private void updateColonies(Star star) throws RequestException {
            boolean needDelete = false;

            final float MIN_POPULATION = 0.0001f;

            String sql = "UPDATE colonies SET" +
                           " focus_population = ?," +
                           " focus_construction = ?," +
                           " focus_farming = ?," +
                           " focus_mining = ?," +
                           " population = ?," +
                           " uncollected_taxes = ?" +
                        " WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                for (BaseColony colony : star.getColonies()) {
                    if (colony.getPopulation() <= MIN_POPULATION) {
                        needDelete = true;
                        continue;
                    }
                    stmt.setDouble(1, colony.getPopulationFocus());
                    stmt.setDouble(2, colony.getConstructionFocus());
                    stmt.setDouble(3, colony.getFarmingFocus());
                    stmt.setDouble(4, colony.getMiningFocus());
                    stmt.setDouble(5, colony.getPopulation());
                    stmt.setDouble(6, colony.getUncollectedTaxes());
                    stmt.setInt(7, ((Colony) colony).getID());
                    stmt.update();
                }
            } catch(Exception e) {
                throw new RequestException(e);
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
                } catch(Exception e) {
                    throw new RequestException(e);
                }

            }
        }

        private void updateFleets(Star star) throws RequestException {
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
                            " time_destroyed = ?" +
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
                    if (fleet.getDestinationStarKey() != null) {
                        stmt.setInt(8, fleet.getDestinationStarID());
                    } else {
                        stmt.setNull(8);
                    }
                    if (fleet.getTargetFleetKey() != null && fleet.getTargetFleetID() != 0) {
                        stmt.setInt(9, fleet.getTargetFleetID());
                    } else {
                        stmt.setNull(9);
                    }
                    stmt.setDateTime(10, fleet.getTimeDestroyed());
                    stmt.setInt(11, fleet.getID());
                    stmt.update();
                }
            } catch(Exception e) {
                throw new RequestException(e);
            }

            if (needInsert) {
                sql = "INSERT INTO fleets (star_id, sector_id, design_id, empire_id, num_ships," +
                                         " stance, state, state_start_time, eta, target_star_id," +
                                         " target_fleet_id, time_destroyed)" +
                     " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                        if (fleet.getDestinationStarKey() != null) {
                            stmt.setInt(10, fleet.getDestinationStarID());
                        } else {
                            stmt.setNull(10);
                        }
                        if (fleet.getTargetFleetKey() != null && fleet.getTargetFleetID() != 0) {
                            stmt.setInt(11, fleet.getTargetFleetID());
                        } else {
                            stmt.setNull(11);
                        }
                        stmt.setDateTime(12, fleet.getTimeDestroyed());
                        stmt.update();
                    }
                } catch(Exception e) {
                    throw new RequestException(e);
                }
            }

            if (needDelete) {
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

        private void updateBuildRequests(Star star) throws RequestException {
            String sql = "UPDATE build_requests SET progress = ?, end_time = ? WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                for (BaseBuildRequest baseBuildRequest : star.getBuildRequests()) {
                    BuildRequest buildRequest = (BuildRequest) baseBuildRequest;
                    stmt.setDouble(1, buildRequest.getProgress(false));
                    stmt.setDateTime(2, buildRequest.getEndTime());
                    stmt.setInt(3, buildRequest.getID());
                    stmt.update();
                }
            } catch(Exception e) {
                throw new RequestException(e);
            }
        }

        private void updateCombatReport(Star star, CombatReport combatReport) throws RequestException {
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
                stmt.setBlob(4, pb.build().toByteArray());
                if (combatReport.getKey() != null) {
                    stmt.setInt(5, Integer.parseInt(combatReport.getKey()));
                }
                stmt.update();
                combatReport.setID(stmt.getAutoGeneratedID());
            } catch(Exception e) {
                throw new RequestException(e);
            }
        }

        private void populateColonies(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT * FROM colonies WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    Colony colony = new Colony(rs);

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
            String sql = "SELECT * FROM fleets WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    Fleet fleet = new Fleet(rs);

                    for (Star star : stars) {
                        if (star.getID() == fleet.getStarID()) {
                            star.getFleets().add(fleet);
                        }
                    }
                }
            }
        }

        private void populateEmpires(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT * FROM empire_presences WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    EmpirePresence empirePresence = new EmpirePresence(rs);

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
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    int starID = rs.getInt("star_id");
                    Star star = null;
                    for (Star thisStar : stars) {
                        if (thisStar.getID() == starID) {
                            star = thisStar;
                            break;
                        }
                    }

                    BuildRequest buildRequest = new BuildRequest(star, rs);
                    star.getBuildRequests().add(buildRequest);
                }
            }
        }

        private void populateBuildings(List<Star> stars, String inClause) throws Exception {
            String sql = "SELECT * FROM buildings WHERE star_id IN "+inClause;
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    Building building = new Building(rs);

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
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    int starID = rs.getInt(1);
                    Messages.CombatReport pb = Messages.CombatReport.parseFrom(rs.getBytes(2));
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
                Colony colony = new ColonyController(getTransaction()).colonize(null, star, planet.getIndex());
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
