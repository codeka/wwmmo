package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseColony;

public class Colony extends BaseColony {
    private int mID;
    private int mSectorID;
    private int mStarID;
    private int mEmpireID;

    public Colony() {
    }
    public Colony(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mKey = Integer.toString(mID);
        mSectorID = rs.getInt("sector_id");
        mStarID = rs.getInt("star_id");
        mStarKey = Integer.toString(mStarID);
        mPlanetIndex = rs.getInt("planet_index");
        mEmpireID = rs.getInt("empire_id");
        if (!rs.wasNull()) {
            mEmpireKey = Integer.toString(mEmpireID);
        }
        mPopulationFocus = rs.getFloat("focus_population");
        mConstructionFocus = rs.getFloat("focus_construction");
        mFarmingFocus = rs.getFloat("focus_farming");
        mMiningFocus = rs.getFloat("focus_mining");
        mUncollectedTaxes = rs.getFloat("uncollected_taxes");
        mCooldownTimeEnd = new DateTime(rs.getTimestamp("cooldown_end_time").getTime());
        mPopulation = rs.getFloat("population");
    }

    public int getSectorID() {
        return mSectorID;
    }
    public int getStarID() {
        return mStarID;
    }
    public int getEmpireID() {
        return mEmpireID;
    }
}
