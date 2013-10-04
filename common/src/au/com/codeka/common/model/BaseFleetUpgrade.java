package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

public class BaseFleetUpgrade {
    protected int mFleetID;
    protected String mUpgradeID;
    protected int mStarID;

    public int getFleetID() {
        return mFleetID;
    }
    public String getUpgradeID() {
        return mUpgradeID;
    }
    public int getStarID() {
        return mStarID;
    }

    public void fromProtocolBuffer(BaseFleet fleet, Messages.FleetUpgrade pb) {
        mFleetID = Integer.parseInt(fleet.getKey());
        mStarID = Integer.parseInt(fleet.getStarKey());
        mUpgradeID = pb.getUpgradeId();
    }

    public void toProtocolBuffer(Messages.FleetUpgrade.Builder pb) {
        pb.setUpgradeId(mUpgradeID);
    }
}
