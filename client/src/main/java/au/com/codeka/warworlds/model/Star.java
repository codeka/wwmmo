package au.com.codeka.warworlds.model;

import org.andengine.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseCombatReport;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.model.designeffects.RadarBuildingEffect;
import au.com.codeka.warworlds.model.designeffects.WormholeDisruptorBuildingEffect;

/** A star is basically a container for planets. It shows up on the starfield list. */
public class Star extends BaseStar {
  private Float radarRange;
  private Float wormholeDisruptorRange;
  private ArrayList<Entity> attachedEntities;

  public Star() {
  }
  @Override
  protected BasePlanet createPlanet(Messages.Planet pb) {
    Planet p = new Planet();
    if (pb != null) {
      p.fromProtocolBuffer(this, pb);
    }
    return p;
  }

  @Override
  protected BaseColony createColony(Messages.Colony pb) {
    Colony c = new Colony();
    if (pb != null) {
      c.fromProtocolBuffer(pb);
    }
    return c;
  }

  @Override
  protected BaseBuilding createBuilding(Messages.Building pb) {
    Building b = new Building();
    if (pb != null) {
      b.fromProtocolBuffer(pb);
    }
    return b;
  }

  @Override
  protected BaseEmpirePresence createEmpirePresence(Messages.EmpirePresence pb) {
    EmpirePresence ep = new EmpirePresence();
    if (pb != null) {
      ep.fromProtocolBuffer(pb);
    }
    return ep;
  }

  @Override
  protected BaseFleet createFleet(Messages.Fleet pb) {
    Fleet f = new Fleet();
    if (pb != null) {
      f.fromProtocolBuffer(pb);
    }
    return f;
  }

  @Override
  protected BaseBuildRequest createBuildRequest(Messages.BuildRequest pb) {
    BuildRequest br = new BuildRequest();
    if (pb != null) {
      br.fromProtocolBuffer(pb);
    }
    return br;
  }

  @Override
  public BaseCombatReport createCombatReport(Messages.CombatReport pb) {
    CombatReport report = new CombatReport();
    if (pb != null) {
      report.fromProtocolBuffer(pb);
    }
    report.setStarKey(mKey);
    return report;
  }

  @Override
  public BaseStar clone() {
    Messages.Star.Builder star_pb = Messages.Star.newBuilder();
    toProtocolBuffer(star_pb);

    Star clone = new Star();
    clone.fromProtocolBuffer(star_pb.build());
    return clone;
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
      System.arraycopy(planets, 0, mPlanets, 0, planets.length);
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
      return wormholeDisruptorRange;
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
      return radarRange;
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


  public List<Entity> getAttachedEntities() {
    if (attachedEntities == null) {
      attachedEntities = new ArrayList<>();
    }
    return attachedEntities;
  }
  public boolean hasAttachedEntities() {
    return attachedEntities != null && attachedEntities.size() > 0;
  }

  public String getCoordinateString() {
    int offsetX = (int)(mOffsetX / (float) Sector.SECTOR_SIZE * 1000.0f);
    if (mSectorX < 0) {
      offsetX = 1000 - offsetX;
    }
    offsetX /= Sector.PIXELS_PER_PARSEC;
    int offsetY = (int)(mOffsetY / (float) Sector.SECTOR_SIZE * 1000.0f);
    if (mSectorY < 0) {
      offsetY = 1000 - offsetY;
    }
    offsetY /= Sector.PIXELS_PER_PARSEC;
    return String.format(Locale.ENGLISH, "[%d.%02d,%d.%02d]",
        mSectorX, offsetX,
        mSectorY, offsetY);
  }
}
