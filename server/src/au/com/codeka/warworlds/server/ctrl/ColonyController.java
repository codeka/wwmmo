package au.com.codeka.warworlds.server.ctrl;

import java.sql.Statement;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.designeffects.TroopCarrierShipEffect;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class ColonyController {
    private final Logger log = LoggerFactory.getLogger(ColonyController.class);
    private DataBase db;

    public ColonyController() {
        db = new DataBase();
    }
    public ColonyController(Transaction trans) {
        db = new DataBase(trans);
    }

    /**
     * Have the given empire attack the given colony (which we assume is part of the given
     * star). We also assume the star has been simulated and is up-to-date.
     * 
     * Attacking colonies is actually quite simple, at least compared with
     * attacking fleets. The number of ships you have with the "troop carrier"
     * effect represents your attack score. The defence score of the colony is
     * 0.25 * it's population * it's defence boost.
     *
     * The number of ships remaining after an attack is:
     * num_ships - (population * 0.25 * defence_bonus)
     * The number of population remaining after an attack is:
     * population - (num_ships * 4 / defence_bonus)
     *
     * This is guaranteed to reduce at least one of the numbers to below zero
     * in which case, which ever has > 0 is the winner. It could also result in
     * both == 0, which is considered a win for the attacking fleet.
     *
     * If the population goes below zero, the colony is destroyed. If the number
     * of ships goes below zero, the colony remains, but with reduce population
     * (hopefully you can rebuild before more ships come!).
     */
    public void attack(int empireID, Star star, Colony colony) throws RequestException {
        float totalTroopCarriers = 0;
        ArrayList<Fleet> troopCarriers = new ArrayList<Fleet>();
        for (BaseFleet baseFleet : star.getFleets()) {
            Fleet fleet = (Fleet) baseFleet;
            if (fleet.getEmpireID() != empireID) {
                continue;
            }
            if (!fleet.getDesign().hasEffect(TroopCarrierShipEffect.class)) {
                continue;
            }
            if (fleet.getState() != Fleet.State.IDLE) {
                continue;
            }
            totalTroopCarriers += fleet.getNumShips();
            troopCarriers.add(fleet);
        }

        float colonyDefence = 0.25f * colony.getPopulation() * colony.getDefenceBoost();
        if (colonyDefence < 1.0f) {
            colonyDefence = 1.0f;
        }

        float remainingShips = totalTroopCarriers - colonyDefence;
        float remainingPopulation = colony.getPopulation() - (totalTroopCarriers * 4.0f / colony.getDefenceBoost());

        if (remainingPopulation <= 0.0f) {
            log.info(String.format("Colony destroyed: remainingPopulation=%.2f, remainingShips=%.2f",
                                   remainingPopulation, remainingShips));
            float numShipsLost = totalTroopCarriers - remainingShips;
            for (Fleet fleet : troopCarriers) {
                float numShips = fleet.getNumShips();
                new FleetController(db.getTransaction()).removeShips(star, fleet, numShipsLost);
                numShipsLost -= numShips;
                if (numShipsLost <= 0.0f) {
                    break;
                }
            }

            try {
                db.destroyColony(colony.getStarID(), colony.getID());
            } catch (Exception e) {
                throw new RequestException(e);
            }
        } else {
            log.info(String.format("Fleets destroyed: remainingPopulation=%.2f, remainingShips=%.2f",
                    remainingPopulation, remainingShips));
            colony.setPopulation(remainingPopulation);
            for (Fleet fleet : troopCarriers) {
                new FleetController(db.getTransaction()).removeShips(star, fleet, fleet.getNumShips());
            }
        }
    }

    public void reducePopulation(Colony colony, float amount) throws RequestException {
        String sql = "UPDATE colonies SET population = GREATEST(0, population - ?) WHERE id = ?";
        try (SqlStmt stmt = db.prepare(sql)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, colony.getID());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    public Colony colonize(Empire empire, Star star, int planetIndex) throws RequestException {
        Colony colony = null;

        // add the initial colony and fleets to the star
        String sql = "INSERT INTO colonies (sector_id, star_id, planet_index, empire_id," +
                                          " focus_population, focus_construction, focus_farming," +
                                          " focus_mining, population, uncollected_taxes," +
                                          " cooldown_end_time)" +
             " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (SqlStmt stmt = db.prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
            colony = new Colony(0, star.getSectorID(), star.getID(), planetIndex,
                    empire == null ? null : empire.getID());
            stmt.setInt(1, colony.getSectorID());
            stmt.setInt(2, colony.getStarID());
            stmt.setInt(3, colony.getPlanetIndex());
            if (colony.getEmpireKey() == null) {
                stmt.setNull(4);
            } else {
                stmt.setInt(4, colony.getEmpireID());
            }
            stmt.setDouble(5, colony.getPopulationFocus());
            stmt.setDouble(6, colony.getConstructionFocus());
            stmt.setDouble(7, colony.getFarmingFocus());
            stmt.setDouble(8, colony.getMiningFocus());
            stmt.setDouble(9, colony.getPopulation());
            stmt.setDouble(10, colony.getUncollectedTaxes());
            stmt.setDateTime(11, colony.getCooldownEndTime());
            stmt.update();
            colony.setID(stmt.getAutoGeneratedID());
        } catch(Exception e) {
            throw new RequestException(e);
        }

        // update the count of colonies in the sector
        if (empire != null) {
            sql = "UPDATE sectors SET num_colonies = num_colonies+1 WHERE id = ?";
            try (SqlStmt stmt = db.prepare(sql)) {
                stmt.setInt(1, star.getSectorID());
                stmt.update();
            } catch(Exception e) {
                throw new RequestException(e);
            }

            sql = "INSERT IGNORE INTO empire_presences" +
                    " (empire_id, star_id, total_goods, total_minerals)" +
                    " VALUES (?, ?, 100, 100)";
            try (SqlStmt stmt = db.prepare(sql)) {
                stmt.setInt(1, empire.getID());
                stmt.setInt(2, star.getID());
                stmt.update();
            } catch(Exception e) {
                throw new RequestException(e);
            }
        }

        star.getColonies().add(colony);
        return colony;
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public void destroyColony(int starID, int colonyID) throws Exception {
            String sql = "DELETE FROM colonies WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, colonyID);
                stmt.update();
            }

            sql = "UPDATE stars SET time_emptied = ? WHERE id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setDateTime(1, DateTime.now());
                stmt.setInt(2, starID);
                stmt.update();
            } catch(Exception e) {
                throw new RequestException(e);
            }
        }
    }
}
