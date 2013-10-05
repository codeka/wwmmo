package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.protobuf.Messages;

public class Fleet extends BaseFleet {
    @Override
    protected BaseFleetUpgrade createUpgrade(Messages.FleetUpgrade pb) {
        return null;
    }
}
