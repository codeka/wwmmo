package au.com.codeka.warworlds.server.ctrl;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.designeffects.WormholeDisruptorBuildingEffect;
import au.com.codeka.warworlds.server.model.BuildingPosition;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

public class WormholeController {
  private static final Log log = new Log("StarController");
  private DataBase db;

  public WormholeController() {
    db = new DataBase();
  }
  public WormholeController(Transaction trans) {
    db = new DataBase(trans);
  }

  public void tuneWormhole(Star srcWormhole, Star destWormhole) throws RequestException {
    // you can only tune wormholes to another wormhole in your alliance
    if (srcWormhole.getWormholeExtra().getEmpireID()
        != destWormhole.getWormholeExtra().getEmpireID()) {
      int srcEmpireID = srcWormhole.getWormholeExtra().getEmpireID();
      int destEmpireID = destWormhole.getWormholeExtra().getEmpireID();
      if (!new AllianceController().isSameAlliance(srcEmpireID, destEmpireID)) {
        throw new RequestException(400);
      }
    }

    srcWormhole.getWormholeExtra().tuneTo(destWormhole.getID());
    new StarController().update(srcWormhole);
  }

  /** Destroy the given wormhole. */
  public void destroyWormhole(Empire empire, Star wormhole) throws RequestException {
    // Make sure there are no other wormhole pointing to us. If there is, make sure they're not
    // pointing to us any more.
    if (empire.getAlliance() != null) {
      List<Star> wormholes = new StarController().getWormholesForAlliance(
          Integer.parseInt(empire.getAlliance().getKey()));
      for (Star otherWormhole : wormholes) {
        if (otherWormhole.getWormholeExtra().getDestWormholeID() == wormhole.getID()) {
          // re-fetch the star with all the info we need.
          Star otherStar = new StarController().getStar(otherWormhole.getID());
          otherStar.getWormholeExtra().tuneTo(0);
          new StarController().update(otherStar);
        }
      }
    }

    try {
      db.destroyWormhole(wormhole.getID());
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  /** Transfer ownership of the given wormhole to the given empire. */
  public void takeOverWormhole(int empireID, Star wormhole) throws RequestException {
    BaseStar.WormholeExtra extra = wormhole.getWormholeExtra();
    extra.setEmpireID(empireID);
    wormhole.setWormholeExtra(extra);
    new StarController().update(wormhole);
  }

  /** Returns {@code true} if the given {@link Star} is within range of a wormhole disruptor. */
  public boolean isInRangeOfWormholeDistruptor(int myEmpireID, Star wormhole)
      throws RequestException {
    // TODO: only +1/-1 if the wormhole is near the edge of the sector.
    long minSectorX = wormhole.getSectorX() - 1;
    long minSectorY = wormhole.getSectorY() - 1;
    long maxSectorX = wormhole.getSectorX() + 1;
    long maxSectorY = wormhole.getSectorY() + 1;

    ArrayList<BuildingPosition> wormholeDisruptors = new BuildingController().getBuildings(
        myEmpireID, minSectorX, minSectorY, maxSectorX, maxSectorY, "wormhole-disruptor");
    for (BuildingPosition wormholeDisruptor : wormholeDisruptors) {
      float distance = Sector.distanceInParsecs(wormhole, wormholeDisruptor.getSectorX(),
          wormholeDisruptor.getSectorY(), wormholeDisruptor.getOffsetX(),
          wormholeDisruptor.getOffsetY());
      BuildingDesign design = wormholeDisruptor.getDesign();
      ArrayList<WormholeDisruptorBuildingEffect> effects =
          design.getEffects(wormholeDisruptor.getLevel(), WormholeDisruptorBuildingEffect.class);
      for (WormholeDisruptorBuildingEffect effect : effects) {
        if (effect.getRange() > distance) {
          return true;
        }
      }
    }

    return false;
  }

  private static class DataBase extends BaseDataBase {
    public DataBase() {
      super();
    }
    public DataBase(Transaction trans) {
      super(trans);
    }

    /** Destroys a wormhole. We delete the fleets, then the star itself. */
    public void destroyWormhole(int starID) throws Exception {
      try(SqlStmt stmt = prepare("DELETE FROM fleet_upgrades WHERE fleet_id IN (SELECT id FROM fleets WHERE star_id = ? OR target_star_id = ?)")) {
        stmt.setInt(1, starID);
        stmt.setInt(2, starID);
        stmt.update();
      }

      try(SqlStmt stmt = prepare("DELETE FROM fleets WHERE star_id = ? OR target_star_id = ?")) {
        stmt.setInt(1, starID);
        stmt.setInt(2, starID);
        stmt.update();
      }

      final String[] sqls = {
          "DELETE FROM scout_reports WHERE star_id = ?",
          "DELETE FROM combat_reports WHERE star_id = ?",
          "DELETE FROM stars WHERE id = ?",
      };
      for (String sql : sqls) {
        try (SqlStmt stmt = prepare(sql)) {
          stmt.setInt(1, starID);
          stmt.update();
        }
      }
    }
  }
}
