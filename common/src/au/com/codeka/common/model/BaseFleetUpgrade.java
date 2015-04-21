package au.com.codeka.common.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import au.com.codeka.common.protobuf.FleetUpgrade;

public class BaseFleetUpgrade {
    protected int mFleetID;
    protected String mUpgradeID;
    protected int mStarID;
    protected String mExtra;

    public int getFleetID() {
        return mFleetID;
    }
    public String getUpgradeID() {
        return mUpgradeID;
    }
    public int getStarID() {
        return mStarID;
    }
    public String getExtra() {
        return mExtra;
    }
    public JsonObject getExtraJson() {
        if (mExtra == null) {
            return null;
        }

        try {
            return new JsonParser().parse(mExtra).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    public void fromProtocolBuffer(BaseFleet fleet, FleetUpgrade pb) {
        mFleetID = Integer.parseInt(fleet.getKey());
        mStarID = Integer.parseInt(fleet.getStarKey());
        mUpgradeID = pb.upgrade_id;
        mExtra = pb.extra;
    }

    public void toProtocolBuffer(FleetUpgrade.Builder pb) {
        pb.upgrade_id = mUpgradeID;
        pb.extra = mExtra;
    }
}
