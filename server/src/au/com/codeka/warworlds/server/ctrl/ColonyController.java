package au.com.codeka.warworlds.server.ctrl;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.designeffects.TroopCarrierShipEffect;
import au.com.codeka.warworlds.server.model.Colony;
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
                db.destroyColony(colony.getID());
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

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public void destroyColony(int colonyID) throws Exception {
            String sql = "DELETE FROM colonies WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, colonyID);
                stmt.update();
            }
        }
    }
}
