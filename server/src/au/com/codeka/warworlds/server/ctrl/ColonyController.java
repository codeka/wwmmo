package au.com.codeka.warworlds.server.ctrl;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.BackgroundRunner;
import au.com.codeka.warworlds.server.Configuration;
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
  private final Log log = new Log("ColonyController");
  private DataBase db;

  public ColonyController() {
    db = new DataBase();
  }

  public ColonyController(Transaction trans) {
    db = new DataBase(trans);
  }

  /**
   * Have the given empire attack the given colony (which we assume is part of the given star). We
   * also assume the star has been simulated and is up-to-date.
   * <p>
   * Attacking colonies is actually quite simple, at least compared with attacking fleets. The
   * number of ships you have with the "troop carrier" effect represents your attack score. The
   * defense score of the colony is 0.25 * it's population * it's defense boost.
   * <p>
   * The number of ships remaining after an attack is:
   * num_ships - (population * 0.25 * defence_bonus)
   * The number of population remaining after an attack is:
   * population - (num_ships * 4 / defence_bonus)
   * <p>
   * This is guaranteed to reduce at least one of the numbers to below zero in which case, which
   * ever has > 0 is the winner. It could also result in both == 0, which is considered a win for
   * the attacking fleet.
   * <p>
   * If the population goes below zero, the colony is destroyed. If the number of ships goes below
   * zero, the colony remains, but with reduced population (hopefully you can rebuild before more
   * ships come!).
   * <p>
   * If the colony is destroyed, then a fraction of the colony's empire's cash is transferred to the
   * attacker. See {@link #getAttackCashValue(Empire, Colony)} for details of the calculation.
   */
  public void attack(int empireID, Star star, Colony colony) throws RequestException {
    ArrayList<Fleet> troopCarriers = new ArrayList<>();
    float totalTroopCarriers = findTroopCarriers(empireID, star, null, troopCarriers);
    float colonyDefence = 0.25f * colony.getPopulation() * colony.getDefenceBoost();
    if (colonyDefence < 1.0f) {
      colonyDefence = 1.0f;
    }

    // You don't have enough troop carriers to attack, do nothing.
    if (totalTroopCarriers < 0.1f) {
      log.info("Empire [%d] Attempt to attack colony with no troop carriers available.", empireID);
      return;
    }

    float remainingShips = totalTroopCarriers - colonyDefence;
    float remainingPopulation = colony.getPopulation()
        - (totalTroopCarriers * 4.0f / colony.getDefenceBoost());
    if (remainingPopulation < 1.0f) {
      remainingPopulation = 0.0f;
    }
    colony.setPopulation(Math.min(0.0f, remainingPopulation));

    Messages.SituationReport.Builder sitrep_pb = null;
    if (colony.getEmpireID() != null) {
      sitrep_pb = Messages.SituationReport.newBuilder();
      sitrep_pb.setRealm(Configuration.i.getRealmName());
      sitrep_pb.setEmpireKey(Integer.toString(colony.getEmpireID()));
      sitrep_pb.setReportTime(DateTime.now().getMillis() / 1000);
      sitrep_pb.setStarKey(star.getKey());
      sitrep_pb.setPlanetIndex(colony.getPlanetIndex());
    }

    if (remainingPopulation <= 0.0f) {
      log.info(String.format(
          Locale.US,
          "Colony destroyed [star=%d %s] [defender=%d] [attacker=%d]: " +
              "remainingPopulation=%.2f, remainingShips=%.2f",
          star.getID(), star.getName(), colony.getEmpireID(), empireID, remainingPopulation,
          remainingShips));
      EmpireController empireController = new EmpireController(db.getTransaction());
      final Empire empire = colony.getEmpireID() == null
          ? null
          : empireController.getEmpire(colony.getEmpireID());

      // Record the colony in the stats for the destroyer.

      new BattleRankController(db.getTransaction())
          .recordColonyDestroyed(empireID, colony.getPopulation());

      // Transfer the cash that results from this to the attacker.
      double cashTransferred = getAttackCashValue(empire, colony);
      log.info(String.format(Locale.US, " - transferring cash: %.2f", cashTransferred));
      if (colony.getEmpireID() != null) {
        empireController.adjustBalance(colony.getEmpireID(), (float) -cashTransferred,
            Messages.CashAuditRecord.newBuilder()
                .setEmpireId(colony.getEmpireID())
                .setColonyId(colony.getID())
                .setReason(Messages.CashAuditRecord.Reason.ColonyDestroyed));
      }
      empireController.adjustBalance(empireID, (float) cashTransferred,
          Messages.CashAuditRecord.newBuilder()
              .setEmpireId(empireID)
              .setColonyId(colony.getID())
              .setReason(Messages.CashAuditRecord.Reason.ColonyDestroyed));

      float numShipsLost = totalTroopCarriers - remainingShips;
      destroyTroopCarriers(star, colony, troopCarriers, numShipsLost, null);

      // Remove any build requests currently in progress in this colony.
      star.getBuildRequests().removeIf(
          buildRequest -> buildRequest.getPlanetIndex() == colony.getPlanetIndex());

      // If this is the last colony for this empire on this star, make sure the empire's home
      // star is reset.
      if (empire != null) {
        maybeResetHomeStar(empire, star, colony);
      }

      if (sitrep_pb != null) {
        Messages.SituationReport.ColonyDestroyedRecord.Builder colony_destroyed_pb =
            Messages.SituationReport.ColonyDestroyedRecord.newBuilder();
        colony_destroyed_pb.setColonyKey(colony.getKey());
        colony_destroyed_pb.setEnemyEmpireKey(Integer.toString(empireID));
        sitrep_pb.setColonyDestroyedRecord(colony_destroyed_pb);
      }
    } else {
      log.info("Fleets destroyed: remainingPopulation=%.2f, remainingShips=%.2f",
          remainingPopulation, remainingShips);
      colony.setPopulation(remainingPopulation);
      destroyTroopCarriers(star, colony, troopCarriers, totalTroopCarriers, sitrep_pb);
    }

    if (sitrep_pb != null) {
      new SituationReportController(db.getTransaction()).saveSituationReport(sitrep_pb.build());
    }
  }

  /**
   * Similar to {@link #attack(int, Star, Colony)}, but instead of destroying the colony, we'll
   * attempt to take over it. This only works for native colonies, if you do this on a non-native
   * colony, your ships will simply be destroyed. If there are not enough ships to take over the
   * colony, again your ships will be destroyed.
   */
  public void sendMissionaries(int empireID, Star star, Colony colony) throws RequestException {
    ArrayList<Fleet> troopCarriers = new ArrayList<>();
    float totalTroopCarriers = findTroopCarriers(empireID, star, "missionary", troopCarriers);
    float colonyDefence = 0.25f * colony.getPopulation() * colony.getDefenceBoost();
    if (colonyDefence < 1.0f) {
      colonyDefence = 1.0f;
    }

    // You don't have any troop carriers at all, do nothing.
    if (totalTroopCarriers < 0.1f) {
      log.info(
          "Empire [%d] Attempt to send missionaries to colony with none available.",
          empireID);
      return;
    }

    float remainingShips = totalTroopCarriers - colonyDefence;
    if (remainingShips < 0.1f || colony.getEmpireKey() != null) {
      // If there's not enough ships, or you're sending the ships to an enemy colony, rather than
      // a native, we'll just destroy your fleets and that's the end of it. As if the colony has
      // destroyed them.
      log.info("Fleets destroyed: remainingShips=%.2f", remainingShips);
      destroyTroopCarriers(star, colony, troopCarriers, totalTroopCarriers, null);
    } else {
      log.info(String.format(
          Locale.US,
          "Colony overtaken [star=%d %s] [defender=%d] [attacker=%d]:",
          star.getID(), star.getName(), colony.getEmpireID(), empireID));
      EmpireController empireController = new EmpireController(db.getTransaction());

      // Record the colony in the stats for the destroyer. I guess...
      new BattleRankController(db.getTransaction())
          .recordColonyDestroyed(empireID, colony.getPopulation());

      // Transfer the cash that results from this to the attacker.
      double cashTransferred = getAttackCashValue(null, colony);
      log.info(String.format(Locale.US, " - transferring cash: %.2f", cashTransferred));
      empireController.adjustBalance(empireID, (float) cashTransferred,
          Messages.CashAuditRecord.newBuilder()
              .setEmpireId(empireID)
              .setColonyId(colony.getID())
              .setReason(Messages.CashAuditRecord.Reason.ColonyDestroyed));

      float numShipsLost = totalTroopCarriers - remainingShips;
      destroyTroopCarriers(star, colony, troopCarriers, numShipsLost, null);

      // Remove any build requests currently in progress in this colony.
      star.getBuildRequests().removeIf(
          buildRequest -> buildRequest.getPlanetIndex() == colony.getPlanetIndex());

      // Before we swap the empireID, make sure we have the right empire presences.
      ensureEmpirePresence(star, empireID);

      // Finally, set the colony's empire ID to ours, we're the owner now!
      colony.setEmpireID(empireID);
    }
  }

  /**
   * When we send emissaries to an enemy colony, we actually just turn it to "propagandizing" state.
   * After some time (based on how many troop carriers you have and the defence of the colony) the
   * troop carriers will be destroyed and the colony will be converted to your own empire.
   */
  public void sendEmissaries(int empireID, Star star, Colony colony) throws RequestException {

  }

  /**
   * Find all the troop carriers with the given upgrade ID (or no upgrades, if upgradeID is null).
   */
  float findTroopCarriers(
      int empireID, Star star, @Nullable String upgradeID, ArrayList<Fleet> troopCarriers) {
    float totalTroopCarriers = 0;
    for (BaseFleet baseFleet : star.getFleets()) {
      Fleet fleet = (Fleet) baseFleet;
      if (fleet.getEmpireID() == null || fleet.getEmpireID() != empireID) {
        continue;
      }
      if (!fleet.getDesign().hasEffect(TroopCarrierShipEffect.class)) {
        continue;
      }
      if (fleet.getState() != Fleet.State.IDLE) {
        continue;
      }
      ArrayList<BaseFleetUpgrade> upgrades = fleet.getUpgrades();
      if (upgradeID == null && (upgrades == null || upgrades.isEmpty())) {
        totalTroopCarriers += fleet.getNumShips();
        troopCarriers.add(fleet);
      } else if (upgradeID != null && upgrades != null) {
        for (BaseFleetUpgrade upgrade : upgrades) {
          if (upgrade.getUpgradeID().equals(upgradeID)) {
            totalTroopCarriers += fleet.getNumShips();
            troopCarriers.add(fleet);
          }
        }
      }
    }
    return totalTroopCarriers;
  }

  private void destroyTroopCarriers(
      Star star, Colony colony, List<Fleet> troopCarriers, float numToDestroy,
      @Nullable Messages.SituationReport.Builder sitrep_pb) throws RequestException {
    float numDestroyed = numToDestroy;

    for (Fleet fleet : troopCarriers) {
      float numShips = fleet.getNumShips();
      new FleetController(db.getTransaction()).removeShips(star, fleet, numToDestroy);
      numToDestroy -= numShips;
      if (numToDestroy <= 0.0f) {
        break;
      }
    }

    if (sitrep_pb != null) {
      Messages.SituationReport.ColonyAttackedRecord.Builder colony_attacked_pb =
          Messages.SituationReport.ColonyAttackedRecord.newBuilder();
      colony_attacked_pb.setColonyKey(colony.getKey());
      colony_attacked_pb.setEnemyEmpireKey(Integer.toString(troopCarriers.get(0).getEmpireID()));
      colony_attacked_pb.setNumShips(numDestroyed);
      sitrep_pb.setColonyAttackedRecord(colony_attacked_pb);
    }
  }

  /**
   * Abandoning a colony will revert it to native. We'll adjust the focus so as not to kill the
   * population if we can. And we'll also remove all buildings and build requests from the colony.
   */
  public void abandon(int empireID, Star star, Colony colony) throws RequestException {
    // Remove any build requests currently in progress in this colony.
    star.getBuildRequests().removeIf(
        buildRequest -> buildRequest.getPlanetIndex() == colony.getPlanetIndex());

    Empire empire = new EmpireController().getEmpire(empireID);
    maybeResetHomeStar(empire, star, colony);

    colony.setAbandoned();
  }

  /**
   * If the given colony is the last one on the star for the given empire, and this is our home
   * star, we'll need to pick another because we're assuming the colony is about to go away.
   */
  private void maybeResetHomeStar(Empire empire, Star star, Colony oldColony) {
    boolean anotherColonyExists = false;
    for (BaseColony baseColony : star.getColonies()) {
      if (baseColony.getEmpireKey() != null && oldColony.getEmpireKey() != null &&
          baseColony.getEmpireKey().equals(oldColony.getEmpireKey()) &&
          !baseColony.getKey().equals(oldColony.getKey())) {
        anotherColonyExists = true;
        break;
      }
    }

    if (!anotherColonyExists && empire.getHomeStarID() == star.getID()) {
      // Do this in a separate thread so as to not deadlock the current transaction. It doesn't
      // matter if there's a delay finding a new home star of a few seconds.
      new BackgroundRunner() {
        @Override
        protected void doInBackground() {
          try {
            new EmpireController().findNewHomeStar(empire.getID(), star.getID());
          } catch (RequestException e) {
            log.error("Error setting home star.", e);
          }
        }
      }.execute();
    }
  }

  /**
   * Calculates the total amount of cash transferred to you if you were to destroy the given colony,
   * owned by the given {@link Empire}.
   * <p>
   * The amount transferred is (where <code>cash</code> is the empire's total cash, minus $250k
   * (which is the new empire starting bonus) and <code>total_colonies</code> is the empire's total
   * number of colonies):
   * <dl>
   *    <dt>If the colony is less than 10 total star, and less than 3 months old,</dt>
   *    <dd>0</dd>
   *    <dt>If the colony had an HQ</dt>
   *    <dd><code>cash</code> * 0.1</dd>
   *    <dt>If the empire has a HQ somewhere else</dt>
   *    <dd><code>cash * 0.9</code> / <code>total_colonies</code></dd>
   *    <dt>If the empire has no HQ</dt>
   *    <dd><code>cash</code> / <code>total_colonies</code></dd>
   * </dl>
   */
  public double getAttackCashValue(@Nullable Empire empire, Colony colony) throws RequestException {
    boolean isHq = false;
    boolean hasHqElsewhere = false;

    if (empire == null) {
      // For native colonies, you get $10k -- except where the colony was recently abandoned, in
      // which case you get nothing.
      if (colony.getAbandonedTime() != null) {
        // If it was abandoned less than two weeks ago, you get nothing.
        Duration diff = new Duration(colony.getAbandonedTime(), DateTime.now());
        if (diff.getStandardDays() <= 14) {
          return 0.0;
        }

        // If it's been more than six weeks, you get the full 10k
        if (diff.getStandardDays() > 42) {
          return 10000.0;
        }

        final double fraction = (diff.getStandardDays() - 14.0) / 28;
        return fraction * 10000.0;
      }
      return 10000.0;
    }

    for (BaseBuilding baseBuilding : colony.getBuildings()) {
      if (baseBuilding.getDesignID().equals("hq")) {
        isHq = true;
      }
    }
    if (!isHq) {
      // If the empire has a HQ, that HQ will by definition be on empire's home star.
      Star homeStar = (Star) empire.getHomeStar();
      for (BaseColony baseColony : homeStar.getColonies()) {
        if (baseColony.getEmpireKey() != null &&
            baseColony.getEmpireKey().equals(empire.getKey())) {
          for (BaseBuilding baseBuilding : baseColony.getBuildings()) {
            if (baseBuilding.getDesignID().equals("hq")) {
              hasHqElsewhere = true;
              break;
            }
          }
        }
        if (hasHqElsewhere) {
          break;
        }
      }
    }

    long numColonies = 1;
    if (empire.getRank() != null) {
      numColonies = empire.getRank().getTotalColonies();
    }
    double totalCash = empire.getCash();
    totalCash -= EmpireController.STARTING_CASH_BONUS; // remove the starting bonus
    if (totalCash < 0) {
      return 0;
    }

    // We'll never take all their cash, leave them about 3/4ths.
    if (numColonies < 4) {
      numColonies = 4;
    }

    if (isHq) {
      return totalCash * 0.1;
    } else if (hasHqElsewhere) {
      return (totalCash * 0.9) / numColonies;
    } else {
      return totalCash / numColonies;
    }
  }

  public void reducePopulation(Colony colony, float amount) throws RequestException {
    String sql = "UPDATE colonies SET population = GREATEST(0, population - ?) WHERE id = ?";
    try (SqlStmt stmt = db.prepare(sql)) {
      stmt.setDouble(1, amount);
      stmt.setInt(2, colony.getID());
      stmt.update();

      colony.setPopulation(colony.getPopulation() - amount);
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  public Colony colonize(
      Empire empire, Star star, int planetIndex, float population, float focusPopulation,
      float focusFarming, float focusMining, float focusConstruction) throws RequestException {
    Colony colony;

    // add the initial colony and fleets to the star
    String sql = "INSERT INTO colonies (sector_id, star_id, planet_index, empire_id," +
        " focus_population, focus_construction, focus_farming," +
        " focus_mining, population, uncollected_taxes," +
        " cooldown_end_time)" +
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (SqlStmt stmt = db.prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
      colony = new Colony(0, star.getSectorID(), star.getID(), planetIndex,
          empire == null ? null : empire.getID(), population, focusPopulation, focusFarming,
          focusMining, focusConstruction);
      stmt.setInt(1, colony.getSectorID());
      stmt.setInt(2, colony.getStarID());
      stmt.setInt(3, colony.getPlanetIndex());
      stmt.setInt(4, colony.getEmpireID());
      stmt.setDouble(5, colony.getPopulationFocus());
      stmt.setDouble(6, colony.getConstructionFocus());
      stmt.setDouble(7, colony.getFarmingFocus());
      stmt.setDouble(8, colony.getMiningFocus());
      stmt.setDouble(9, colony.getPopulation());
      stmt.setDouble(10, colony.getUncollectedTaxes());
      stmt.setDateTime(11, colony.getCooldownEndTime());
      stmt.update();
      colony.setID(stmt.getAutoGeneratedID());
    } catch (Exception e) {
      throw new RequestException(e);
    }

    // update the count of colonies in the sector
    if (empire != null) {
      sql = "UPDATE sectors SET num_colonies = num_colonies+1 WHERE id = ?";
      try (SqlStmt stmt = db.prepare(sql)) {
        stmt.setInt(1, star.getSectorID());
        stmt.update();
      } catch (Exception e) {
        throw new RequestException(e);
      }

      ensureEmpirePresence(star, empire.getID());
    }

    // make sure the star is no longer marked abandoned!
    sql = "DELETE FROM abandoned_stars WHERE star_id = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setInt(1, star.getID());
      stmt.update();
    } catch (Exception e) {
      throw new RequestException(e);
    }

    star.getColonies().add(colony);
    return colony;
  }

  /**
   * Make sure we have the empire presences for all the empires on the star.
   */
  private void ensureEmpirePresence(Star star, int empireID) throws RequestException {
    // Only insert a new empire_presences row if there's not one already there.
    int numExistingColonies = 0;
    for (BaseColony baseColony : star.getColonies()) {
      Colony existingColony = (Colony) baseColony;
      if (existingColony.getEmpireID() != null &&
          existingColony.getEmpireID() == empireID) {
        numExistingColonies++;
      }
    }

    if (numExistingColonies == 0) {
      String sql = "INSERT INTO empire_presences" +
          " (empire_id, star_id, total_goods, total_minerals)" +
          " VALUES (?, ?, 100, 100)";
      try (SqlStmt stmt = db.prepare(sql)) {
        stmt.setInt(1, empireID);
        stmt.setInt(2, star.getID());
        stmt.update();
      } catch (Exception e) {
        throw new RequestException(e);
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

    public void destroyColony(int starID, int colonyID) throws Exception {
      String sql = "DELETE FROM build_requests WHERE colony_id = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setInt(1, colonyID);
        stmt.update();
      }
      
      sql = "DELETE FROM buildings WHERE colony_id = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setInt(1, colonyID);
        stmt.update();
      }

      sql = "DELETE FROM colonies WHERE id = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setInt(1, colonyID);
        stmt.update();
      }

      sql = "UPDATE stars SET time_emptied = ? WHERE id = ?";
      try (SqlStmt stmt = DB.prepare(sql)) {
        stmt.setDateTime(1, DateTime.now());
        stmt.setInt(2, starID);
        stmt.update();
      } catch (Exception e) {
        throw new RequestException(e);
      }
    }
  }
}
