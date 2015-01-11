package au.com.codeka.warworlds.model;

import java.util.ArrayList;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.warworlds.game.starfield.WormholeDisruptorIndicatorEntity;
import au.com.codeka.warworlds.model.designeffects.RadarBuildingEffect;
import au.com.codeka.warworlds.model.designeffects.WormholeDisruptorBuildingEffect;

/** A star is basically a container for planets. It shows up on the starfield list. */
public class Star extends StarSummary {
  private Float radarRange;
  private Float wormholeDisruptorRange;

  public Star() {
  }

  public Star(StarType type, String name, int size, long sectorX, long sectorY, int offsetX,
      int offsetY, Planet[] planets) {
    mKey = "0";
    mStarType = type;
    mName = name;
    mSize = size;
    mSectorX = sectorX;
    mSectorY = sectorY;
    mOffsetX = offsetX;
    mOffsetY = offsetY;
    if (planets == null) {
      mPlanets = new BasePlanet[0];
    } else {
      mPlanets = new BasePlanet[planets.length];
      for (int i = 0; i < planets.length; i++) {
        mPlanets[i] = planets[i];
      }
    }
    mColonies = new ArrayList<>();
    mEmpires = new ArrayList<>();
    mFleets = new ArrayList<>();
    mBuildRequests = new ArrayList<>();
  }

  public int getID() {
    return Integer.parseInt(mKey);
  }

  /**
   * If this star has a wormhole disruptor building on it for the given empire, we'll return the
   * range (in parsecs) of the wormhole disruptor. If they don't have one, we'll return 0.
   */
  public float getWormholeDisruptorRange(String empireKey) {
    if (wormholeDisruptorRange != null) {
      return wormholeDisruptorRange.floatValue();
    }

    wormholeDisruptorRange = 0.0f;
    for (BaseColony baseColony : getColonies()) {
      if (baseColony.getEmpireKey() == null) {
        continue;
      }
      if (baseColony.getEmpireKey().equals(empireKey)) {
        for (BaseBuilding baseBuilding : baseColony.getBuildings()) {
          Building building = (Building) baseBuilding;
          BuildingDesign design = building.getDesign();
          ArrayList<WormholeDisruptorBuildingEffect> effects =
              design.getEffects(building.getLevel(), WormholeDisruptorBuildingEffect.class);
          for (WormholeDisruptorBuildingEffect effect : effects) {
            if (wormholeDisruptorRange < effect.getRange()) {
              wormholeDisruptorRange = effect.getRange();
            }
          }
        }
      }
    }

    return wormholeDisruptorRange;
  }

  /**
   * If this star has a radar building on it for the given empire, we'll return the range
   * (in parsecs) of the radar. If they don't have one, we'll return 0.
   */
  public float getRadarRange(String empireKey) {
    if (radarRange != null) {
      return radarRange.floatValue();
    }

    radarRange = 0.0f;
    for (BaseColony baseColony : getColonies()) {
      if (baseColony.getEmpireKey() == null) {
        continue;
      }
      if (baseColony.getEmpireKey().equals(empireKey)) {
        for (BaseBuilding baseBuilding : baseColony.getBuildings()) {
          Building building = (Building) baseBuilding;
          BuildingDesign design = building.getDesign();
          for (RadarBuildingEffect effect : design
              .getEffects(building.getLevel(), RadarBuildingEffect.class)) {
            if (radarRange < effect.getRange()) {
              radarRange = effect.getRange();
            }
          }
        }
      }
    }

    return radarRange;
  }
}
