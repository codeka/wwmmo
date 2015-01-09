package au.com.codeka.warworlds.server.ctrl;

import java.util.ArrayList;

import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.designeffects.WormholeDisruptorBuildingEffect;
import au.com.codeka.warworlds.server.model.BuildingPosition;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

public class WormholeController {
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
}
