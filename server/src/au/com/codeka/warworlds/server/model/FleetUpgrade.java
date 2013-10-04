package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import au.com.codeka.common.model.BaseFleetUpgrade;

public class FleetUpgrade extends BaseFleetUpgrade {
    public FleetUpgrade() {
    }
    public FleetUpgrade(ResultSet rs) throws SQLException {
        mFleetID = rs.getInt("fleet_id");
        mUpgradeID = rs.getString("upgrade_id");
        mStarID = rs.getInt("star_id");
    }
}
