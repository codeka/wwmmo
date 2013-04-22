package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.EmpirePresence;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class StarController {

    public Star getStar(int id) throws RequestException {
        return DataBase.getStars(new int[] {id}).get(0);
    }

    public List<Star> getStars(int[] ids) throws RequestException {
        return DataBase.getStars(ids);
    }

    public void update(Star star) throws RequestException {
        DataBase.updateStar(star);
    }

    private static class DataBase extends BaseDataBase {
        public static List<Star> getStars(int[] ids) throws RequestException {
            final String sql = "SELECT stars.id, sector_id, name, sectors.x AS sector_x," +
                                     " sectors.y AS sector_y, stars.x, stars.y, size, star_type, planets," +
                                     " last_simulation, time_emptied" +
                              " FROM stars" +
                              " INNER JOIN sectors ON stars.sector_id = sectors.id" +
                              " WHERE stars.id IN "+buildInClause(ids);

            try (SqlStmt stmt = DB.prepare(sql)) {
                ResultSet rs = stmt.select();

                ArrayList<Star> stars = new ArrayList<Star>();
                while (rs.next()) {
                    stars.add(new Star(rs));
                }
                populateStars(stars);
                return stars;
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        public static void updateStar(Star star) throws RequestException {
            final String sql = "UPDATE stars SET" +
                                 " last_simulation = ?" +
                              " WHERE id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setDateTime(1, star.getLastSimulation());
                stmt.setInt(2, star.getID());
                stmt.update();

                updateEmpires(star);
                updateColonies(star);
                updateFleets(star);
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        private static void updateEmpires(Star star) throws RequestException {
            final String sql = "UPDATE empire_presences SET" +
                                 " total_goods = ?," +
                                 " total_minerals = ?" +
                              " WHERE id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                for (BaseEmpirePresence empire : star.getEmpires()) {
                    stmt.setDouble(1, empire.getTotalGoods());
                    stmt.setDouble(2, empire.getTotalMinerals());
                    stmt.setInt(3, ((EmpirePresence) empire).getID());
                    stmt.update();
                }
            } catch(Exception e) {
               throw new RequestException(500, e);
            }
        }

        private static void updateColonies(Star star) throws RequestException {
            final String sql = "UPDATE colonies SET" +
                                 " focus_population = ?," +
                                 " focus_construction = ?," +
                                 " focus_farming = ?," +
                                 " focus_mining = ?," +
                                 " population = ?," +
                                 " uncollected_taxes = ?" +
                              " WHERE id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                for (BaseColony colony : star.getColonies()) {
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
                throw new RequestException(500, e);
            }
        }

        private static void updateFleets(Star star) throws RequestException {
            final String sql = "UPDATE fleets SET" +
                                 " num_ships = ?," +
                                 " stance = ?," +
                                 " state = ?," +
                                 " state_start_time = ?," +
                                 " eta = ?," +
                                 " target_star_id = ?," +
                                 " target_fleet_id = ?," +
                                 " time_destroyed = ?" +
                              " WHERE id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                for (BaseFleet baseFleet : star.getFleets()) {
                    Fleet fleet = (Fleet) baseFleet;
                    stmt.setDouble(1, fleet.getNumShips());
                    stmt.setInt(2, fleet.getStance().getValue());
                    stmt.setInt(3, fleet.getState().getValue());
                    stmt.setDateTime(4, fleet.getStateStartTime());
                    stmt.setDateTime(5, fleet.getEta());
                    if (fleet.getDestinationStarKey() != null) {
                        stmt.setInt(6, fleet.getDestinationStarID());
                    } else {
                        stmt.setNull(6);
                    }
                    if (fleet.getTargetFleetKey() != null) {
                        stmt.setInt(7, fleet.getTargetFleetID());
                    } else {
                        stmt.setNull(7);
                    }
                    stmt.setDateTime(8, null); // todo
                    stmt.setInt(9, fleet.getID());
                    stmt.update();
                }
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        private static void populateStars(List<Star> stars) throws RequestException {
            int[] starIds = new int[stars.size()];
            for (int i = 0; i < stars.size(); i++) {
                Star star = stars.get(i);
                star.setColonies(new ArrayList<BaseColony>());
                star.setFleets(new ArrayList<BaseFleet>());
                star.setEmpires(new ArrayList<BaseEmpirePresence>());
                star.setBuildRequests(new ArrayList<BaseBuildRequest>());
                starIds[i] = star.getID();
            }

            String sql = "SELECT colonies.*" +
                        " FROM colonies" +
                        " WHERE star_id IN "+buildInClause(starIds);
            try (SqlStmt stmt = DB.prepare(sql)) {
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
            } catch(Exception e) {
                throw new RequestException(500, e);
            }

            sql = "SELECT fleets.*" +
                 " FROM fleets" +
                 " WHERE star_id IN "+buildInClause(starIds);
            try (SqlStmt stmt = DB.prepare(sql)) {
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    Fleet fleet = new Fleet(rs);

                    for (Star star : stars) {
                        if (star.getID() == fleet.getStarID()) {
                            star.getFleets().add(fleet);
                        }
                    }
                }
            } catch(Exception e) {
                throw new RequestException(500, e);
            }

            sql = "SELECT empire_presences.*" +
                 " FROM empire_presences" +
                 " WHERE empire_presences.star_id IN "+buildInClause(starIds);
            try (SqlStmt stmt = DB.prepare(sql)) {
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    EmpirePresence empire = new EmpirePresence(rs);

                    for (Star star : stars) {
                        if (star.getID() == empire.getStarID()) {
                            // by default, you get 500 max goods/minerals
                            empire.setMaxGoods(500);
                            empire.setMaxMinerals(500);

                            star.getEmpires().add(empire);
                        }
                    }
                }
            } catch(Exception e) {
                throw new RequestException(500, e);
            }

            // TODO: build requests
        }
    }
}
