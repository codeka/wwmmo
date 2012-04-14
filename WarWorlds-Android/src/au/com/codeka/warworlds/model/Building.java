package au.com.codeka.warworlds.model;

/**
 * Represents a single building on a colony.
 */
public class Building {
    private String mKey;
    private String mColonyKey;
    private String mDesignName;

    public String getKey() {
        return mKey;
    }
    public String getColonyKey() {
        return mColonyKey;
    }
    public String getDesignName() {
        return mDesignName;
    }
    public BuildingDesign getDesign() {
        return BuildingDesignManager.getInstance().getDesign(mDesignName);
    }

    public static Building fromProtocolBuffer(warworlds.Warworlds.Building pb) {
        Building building = new Building();
        building.mKey = pb.getKey();
        building.mColonyKey = pb.getColonyKey();
        building.mDesignName = pb.getDesignName();
        return building;
    }
}
