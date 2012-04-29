package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

public class ShipDesignManager extends DesignManager {
    private static ShipDesignManager sInstance = new ShipDesignManager();
    public static ShipDesignManager getInstance() {
        return sInstance;
    }

    /**
     * Gets the \c ShipDesign with the given identifier.
     */
    public ShipDesign getDesign(String designID) {
        return (ShipDesign) super.getDesign(designID);
    }

    protected String getDesignUrl() {
        return "/data/ships.xml";
    }

    protected Design parseDesign(Element designElement) {
        return new ShipDesign.Factory(designElement).get();
    }
}
