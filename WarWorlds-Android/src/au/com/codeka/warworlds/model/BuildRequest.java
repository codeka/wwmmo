package au.com.codeka.warworlds.model;

/**
 * Represents an in-progress build order.
 */
public class BuildRequest {
    private BuildKind mBuildKind;
    private String mDesignName;
    private String mColonyKey;

    public BuildKind getBuildKind() {
        return mBuildKind;
    }
    public String getDesignName() {
        return mDesignName;
    }
    public BuildingDesign getBuildingDesign() {
        if (mBuildKind != BuildKind.BUILDING) {
            throw new IllegalArgumentException("Cannot getBuildingDesign when BuildKind != BUILDING");
        }
        return BuildingDesignManager.getInstance().getDesign(mDesignName);
    }
    public String getColonyKey() {
        return mColonyKey;
    }

    public static BuildRequest fromProtocolBuffer(warworlds.Warworlds.BuildRequest pb) {
        BuildRequest request = new BuildRequest();
        request.mBuildKind = BuildKind.fromNumber(pb.getBuildKind().getNumber());
        request.mDesignName = pb.getDesignName();
        request.mColonyKey = pb.getColonyKey();
        return request;
    }

    public enum BuildKind {
        BUILDING(0),
        SHIP(1);

        private int mValue;

        BuildKind(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static BuildKind fromNumber(int value) {
            for(BuildKind bk : BuildKind.values()) {
                if (bk.getValue() == value) {
                    return bk;
                }
            }

            return BuildKind.BUILDING;

        }
    }
}
