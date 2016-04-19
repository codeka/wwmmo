package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;

/**
 * Represents a single building on a colony.
 */
public class Building extends BaseBuilding {
    public BuildingDesign getDesign() {
        return (BuildingDesign) BaseDesignManager.i.getDesign(DesignKind.BUILDING, mDesignID);
    }
}
