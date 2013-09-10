package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.warworlds.model.designeffects.RadarBuildingEffect;


/**
 * A star is \i basically a container for planets. It shows up on the starfield list.
 */
public class Star extends StarSummary {
    private Float mRadarRange;

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
