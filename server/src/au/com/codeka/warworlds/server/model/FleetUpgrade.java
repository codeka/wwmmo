package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.protobuf.Messages;

public class FleetUpgrade extends BaseFleetUpgrade {
    public FleetUpgrade() {
    }
    protected FleetUpgrade(ResultSet rs) throws SQLException {
        mFleetID = rs.getInt("fleet_id");
        mUpgradeID = rs.getString("upgrade_id");
        mStarID = rs.getInt("star_id");
        mExtra = rs.getString("extra");
    }
    public FleetUpgrade(int starID, int fleetID, String upgradeID) {
        mFleetID = fleetID;
        mStarID = starID;
        mUpgradeID = upgradeID;
    }
    public FleetUpgrade(FleetUpgrade copy) {
        mFleetID = copy.mFleetID;
        mUpgradeID = copy.mUpgradeID;
        mStarID = copy.mStarID;
    }

    public static FleetUpgrade create(ResultSet rs) throws SQLException {
        String id = rs.getString("upgrade_id");
        if (id.equals("boost")) {
            return new BoostFleetUpgrade(rs);
        } else {
            return new FleetUpgrade(rs);
        }
    }

    public void setFleetID(int id) {
        mFleetID = id;
    }

    /** This is called when the fleet that owns us arrives at a star. */
    public void onArrived(Star star, Fleet fleet) {
    }

    /** This is a specific upgrade for the 'boost' upgrade. */
    public static class BoostFleetUpgrade extends FleetUpgrade {
        private boolean mIsBoosting;

        public BoostFleetUpgrade() {
        }
        public BoostFleetUpgrade(ResultSet rs) throws SQLException {
            super(rs);
            parseExtra();
        }

        public boolean isBoosting() {
            return mIsBoosting;
        }

        public void isBoosting(boolean value) {
            mIsBoosting = value;
        }

        /**
         * Boost is a one-off thing. If you want to boost again, you have to upgrade again. So if we
         * were boosting, then we'll remove this upgrade from the fleet.
         */
        @Override
        public void onArrived(Star star, Fleet fleet) {
            if (isBoosting()) {
               fleet.getUpgrades().remove(this);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public String getExtra() {
            JSONObject json = new JSONObject();
            json.put("is_boosting", new Boolean(mIsBoosting));
            return json.toString();
        }

        @Override
        public void fromProtocolBuffer(BaseFleet fleet, Messages.FleetUpgrade pb) {
            super.fromProtocolBuffer(fleet, pb);
            parseExtra();
        }

        private void parseExtra() {
            JSONObject extra = getExtraJson();
            if (extra != null && extra.containsKey("is_boosting")) {
                mIsBoosting = (Boolean) extra.get("is_boosting");
            }
        }
    }
}

