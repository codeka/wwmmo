package au.com.codeka.warworlds.model;

import au.com.codeka.common.design.BuildingDesign;
import au.com.codeka.common.model.Building;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.EmpirePresence;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.model.designeffects.RadarBuildingEffect;


/**
 * A star is \i basically a container for planets. It shows up on the starfield list.
 */
public class StarHelper {
    /**
     * If this star has a radar building on it for the given empire, we'll return the range
     * (in parsecs) of the radar. If they don't have one, we'll return 0.
     */
    public static float getRadarRange(Star star, String empireKey) {
        float radarRange = 0.0f;
        for (Building building : star.buildings) {
            BuildingDesign design = DesignManager.i.getDesign(building);
            for (RadarBuildingEffect effect : design.getEffects(building.level, RadarBuildingEffect.class)) {
                if (radarRange < effect.getRange()) {
                    // make sure the building belongs to this empire...
                    boolean thisEmpire = false;
                    for (Colony colony : star.colonies) {
                        if (colony.key.equals(building.colony_key) && colony.empire_key != null &&
                                colony.empire_key.equals(empireKey)) {
                            thisEmpire = true;
                            break;
                        }
                    }
                    if (thisEmpire) {
                        radarRange = effect.getRange();
                    }
                }
            }
        }

        return radarRange;
    }

    public static EmpirePresence getEmpire(Star star, String empireKey) {
        for (EmpirePresence empire : star.empires) {
            if (empire.key != null && empire.key.equals(empireKey)) {
                return empire;
            }
        }
        return null;
    }
}

