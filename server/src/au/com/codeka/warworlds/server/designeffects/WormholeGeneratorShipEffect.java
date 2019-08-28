package au.com.codeka.warworlds.server.designeffects;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.ShipEffect;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class WormholeGeneratorShipEffect extends ShipEffect {
  private final Log log = new Log("WormholeGeneratorShipEffect");

  /**
   * This is called when we arrive on a star. Assuming the star is a marker, we'll convert it to a wormhole!
   */
  @Override
  public void onArrived(BaseStar baseStar, BaseFleet baseFleet) {
    Star star = (Star) baseStar;
    Fleet fleet = (Fleet) baseFleet;

    if (!star.getStarType().getInternalName().equals("marker")) {
      log.warning("We arrived at a non-marker star -- this should be impossible!");
      return;
    }

    String name = "Wormhole";
    if (fleet.getNotes() != null && fleet.getNotes().length() > 0) {
      name = fleet.getNotes().replace("\n", " ").replace("\r", "");
    }

    star.setStarType(Star.getStarType(Star.Type.Wormhole));
    star.setName(name);
    star.setWormholeExtra(new Star.WormholeExtra(fleet.getEmpireID()));

    // TODO: probably not the best place for this to go...
    String sql = "DELETE FROM fleet_upgrades WHERE fleet_id = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setInt(1, fleet.getID());
      stmt.update();
    } catch (Exception e) {
      log.error("", e);
    }

    sql = "DELETE FROM fleets WHERE id = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setInt(1, fleet.getID());
      stmt.update();
    } catch (Exception e) {
      log.error("Error removing wormhole generator.", e);
    }
    star.getFleets().remove(fleet);
  }
}
