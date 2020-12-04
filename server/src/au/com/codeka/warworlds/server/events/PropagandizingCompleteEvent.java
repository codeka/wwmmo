package au.com.codeka.warworlds.server.events;

import org.joda.time.DateTime;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.Event;
import au.com.codeka.warworlds.server.RequestContext;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.BattleRankController;
import au.com.codeka.warworlds.server.ctrl.ColonyController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.FleetController;
import au.com.codeka.warworlds.server.ctrl.SituationReportController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.CombatReport;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.ScoutReport;
import au.com.codeka.warworlds.server.model.Star;

public class PropagandizingCompleteEvent extends Event {
  private final static Log log = new Log("PropagandizingCompleteEvent");

  @Override
  protected String getNextEventTimeSql() {
    return "SELECT MIN(eta) FROM fleets WHERE state = " + BaseFleet.State.PROPAGANDIZING.getValue();
  }

  @Override
  public void process() {
    HashSet<Integer> invalidFleets = new HashSet<>();
    String sql = "SELECT id, star_id, target_star_id FROM fleets WHERE eta < ? AND state = "
        + BaseFleet.State.PROPAGANDIZING.getValue();
    log.debug("SQL: %s", sql);
    try (SqlStmt stmt = DB.prepare(sql)) {
      // Anything in the next 10 seconds is a candidate.
      stmt.setDateTime(1, DateTime.now().plusSeconds(10));
      SqlResult res = stmt.select();
      while (res.next()) {
        int fleetID = res.getInt(1);
        int starID = res.getInt(2);
        int colonyID = res.getInt(3);
        log.debug("Processing fleet %d", fleetID);

        RequestContext.i.setContext("event: PropagandizingCompleteEvent fleet.id=" + fleetID);

        Star star = new StarController().getStar(starID);
        try {
          if (!processFleet(fleetID, star, colonyID)) {
            // If it failed to process, we'll want to reset the fleet to IDLE state so that we
            // don't keep trying to process it over and over.
            invalidFleets.add(fleetID);
          }
        } catch (Exception e) {
          log.error("Error processing fleet-move event!", e);
        }
      }
    } catch (Exception e) {
      log.error("Error processing fleet-move event!", e);
    }

    for (Integer fleetID : invalidFleets) {
      sql = "UPDATE fleets SET state = " + BaseFleet.State.IDLE.getValue() + ", eta = NULL WHERE id = ?";
      try (SqlStmt stmt = DB.prepare(sql)) {
        stmt.setInt(1, fleetID);
        stmt.update();
      } catch (SQLException e) {
        log.error("Error resetting invalid fleet.", e);
      }
    }
  }

  public static boolean processFleet(
      int fleetID, Star star, int colonyID) throws RequestException {
    Simulation sim = new Simulation();
    sim.simulate(star);

    // Remove the fleet from the star, it's job is done.
    Fleet fleet = null;
    for (BaseFleet baseFleet : star.getFleets()) {
      if (((Fleet) baseFleet).getID() == fleetID) {
        fleet = (Fleet) baseFleet;
        break;
      }
    }
    if (fleet == null) {
      // It's already been destroyed or something like that... nothing to do.
      log.debug("fleet is null");
      return false;
    }

    // Now, make sure we have the right colony.
    Colony colony = star.getColony(colonyID);
    if (colony == null || (
        colony.getEmpireID() != null && colony.getEmpireID().equals(fleet.getEmpireID()))) {
      // The colony doesn't exist any more, it's our or it's gone native...
      // TODO: what if some other player has taken it before us?
      log.debug("Colony doesn't seem to exist?");
      return false;
    }

    // Remove the fleet.
    int empireID = fleet.getEmpireID();
    new FleetController().removeShips(star, fleet, fleet.getNumShips());

    log.info(String.format(
        Locale.US,
        "Colony overcome by propaganda [star=%d %s] [defender=%d] [attacker=%d]:",
        star.getID(), star.getName(), colony.getEmpireID(), empireID));
    EmpireController empireController = new EmpireController();

    // Record the colony in the stats for the destroyer. I guess...
    new BattleRankController()
        .recordColonyDestroyed(empireID, colony.getPopulation());

    // Before we swap the empireID, make sure we have the right empire presences.
    new ColonyController().transferColonyOwnership(star, colony, empireID);

    new StarController().update(star);

    return true;
  }
}
