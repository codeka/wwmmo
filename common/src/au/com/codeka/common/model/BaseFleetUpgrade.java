package au.com.codeka.common.model;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import au.com.codeka.common.protobuf.Messages;

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
    public JSONObject getExtraJson() {
        if (mExtra == null) {
            return null;
        }

        try {
            return (JSONObject) new JSONParser().parse(mExtra);
        } catch (Exception e) {
            return null;
        }
    }

    public void fromProtocolBuffer(BaseFleet fleet, Messages.FleetUpgrade pb) {
        mFleetID = Integer.parseInt(fleet.getKey());
        mStarID = Integer.parseInt(fleet.getStarKey());
        mUpgradeID = pb.getUpgradeId();
        mExtra = pb.getExtra();
    }

    public void toProtocolBuffer(Messages.FleetUpgrade.Builder pb) {
        pb.setUpgradeId(mUpgradeID);
        if (mExtra != null) {
            pb.setExtra(mExtra);
        }
    }
}
