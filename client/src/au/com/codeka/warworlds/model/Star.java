package au.com.codeka.warworlds.model;

import java.util.ArrayList;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.warworlds.model.designeffects.RadarBuildingEffect;


/**
 * A star is \i basically a container for planets. It shows up on the starfield list.
 */
public class Star extends StarSummary {
    private Float mRadarRange;

    public Star() {
    }
    public Star(StarType type, String name, int size, long sectorX, long sectorY, int offsetX, int offsetY,
            Planet[] planets) {
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
        mColonies = new ArrayList<BaseColony>();
        mEmpires = new ArrayList<BaseEmpirePresence>();
        mFleets = new ArrayList<BaseFleet>();
        mBuildRequests = new ArrayList<BaseBuildRequest>();
    }

    /**
     * If this star has a radar building on it for the given empire, we'll return the range
     * (in parsecs) of the radar. If they don't have one, we'll return 0.
     */
    public float getRadarRange(String empireKey) {
        if (mRadarRange != null) {
            return (float) mRadarRange;
        }

        mRadarRange = 0.0f;
        for (BaseColony baseColony : getColonies()) {
            if (baseColony.getEmpireKey() == null) {
                continue;
            }
            if (baseColony.getEmpireKey().equals(empireKey)) {
                for (BaseBuilding baseBuilding : baseColony.getBuildings()) {
                    Building building = (Building) baseBuilding;
                    BuildingDesign design = building.getDesign();
                    for (RadarBuildingEffect effect : design.getEffects(building.getLevel(), RadarBuildingEffect.class)) {
                        if (mRadarRange < effect.getRange()) {
                            mRadarRange = effect.getRange();
                        }
                    }
                }
            }
        }

        return mRadarRange;
    }
}
