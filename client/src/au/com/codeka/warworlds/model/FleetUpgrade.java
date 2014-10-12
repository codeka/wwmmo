package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.protobuf.Messages;

import com.google.gson.JsonObject;

public class FleetUpgrade extends BaseFleetUpgrade {

    /** This is a specific upgrade for the 'boost' upgrade. */
    public static class BoostFleetUpgrade extends FleetUpgrade {
        private boolean mIsBoosting;

        public boolean isBoosting() {
            return mIsBoosting;
        }

        @Override
        public void fromProtocolBuffer(BaseFleet fleet, Messages.FleetUpgrade pb) {
            super.fromProtocolBuffer(fleet, pb);

            JsonObject extra = getExtraJson();
            if (extra != null && extra.has("is_boosting")) {
                mIsBoosting = extra.get("is_boosting").getAsBoolean();
            }
        }
    }
}
