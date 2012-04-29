package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

public class BuildingDesignManager extends DesignManager {
    private static BuildingDesignManager sInstance = new BuildingDesignManager();
    public static BuildingDesignManager getInstance() {
        return sInstance;
    }

    /**
     * Gets the \c BuildingDesign with the given identifier.
     */
    public BuildingDesign getDesign(String designID) {
        return (BuildingDesign) super.getDesign(designID);
    }

    protected String getDesignUrl() {
        return "/data/buildings.xml";
    }

    protected Design parseDesign(Element designElement) {
        return new BuildingDesign.Factory(designElement).get();
    }
}
