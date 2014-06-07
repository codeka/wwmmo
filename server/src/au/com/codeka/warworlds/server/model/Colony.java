package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;
import java.util.ArrayList;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.warworlds.server.data.SqlResult;

public class Colony extends BaseColony {
    private int mID;
    private int mSectorID;
    private int mStarID;
    private Integer mEmpireID;

    public Colony() {
        mBuildings = new ArrayList<BaseBuilding>();
    }
    public Colony(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mKey = Integer.toString(mID);
        mSectorID = res.getInt("sector_id");
        mStarID = res.getInt("star_id");
        mStarKey = Integer.toString(mStarID);
        mPlanetIndex = res.getInt("planet_index");
        mEmpireID = res.getInt("empire_id");
        if (mEmpireID != null) {
            mEmpireKey = Integer.toString(mEmpireID);
        }
        mPopulationFocus = res.getFloat("focus_population");
        mConstructionFocus = res.getFloat("focus_construction");
        mFarmingFocus = res.getFloat("focus_farming");
        mMiningFocus = res.getFloat("focus_mining");
        mUncollectedTaxes = res.getFloat("uncollected_taxes");
        mCooldownTimeEnd = res.getDateTime("cooldown_end_time");
        mPopulation = res.getFloat("population");
        mDefenceBoost = 1.0f;
        mBuildings = new ArrayList<BaseBuilding>();
    }
    public Colony(int id, int sectorID, int starID, int planetIndex, Integer empireID,
            float population) {
        mID = id;
        mKey = Integer.toString(mID);
        mSectorID = sectorID;
        mStarID = starID;
        mStarKey = Integer.toString(mStarID);
        mPlanetIndex = planetIndex;
        if (empireID != null) {
            mEmpireID = empireID;
            mEmpireKey = Integer.toString(mEmpireID);
        }
        mPopulationFocus = 0.25f;
        mConstructionFocus = 0.25f;
        mFarmingFocus = 0.25f;
        mMiningFocus = 0.25f;
        mUncollectedTaxes = 0.0f;
        mCooldownTimeEnd = new DateTime().plusHours(8);
        mPopulation = population;
        mDefenceBoost = 1.0f;
        mBuildings = new ArrayList<BaseBuilding>();
    }

    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(id);
    }
    public void setMaxPopulation(float maxPopulation) {
        mMaxPopulation = maxPopulation;
    }
    public void setDefenceBoost(float boost) {
        mDefenceBoost = boost;
    }

    public int getID() {
        return mID;
    }
    public int getSectorID() {
        return mSectorID;
    }
    public int getStarID() {
        return mStarID;
    }
    public Integer getEmpireID() {
        return mEmpireID;
    }

    /**
     * Sanitize this colony for viewing by other empires. Basically, we just zero-out various
     * "sensitive" bits of data.
     */
    public void sanitize() {
        mPopulationDelta = 0.0f;
        mGoodsDelta = 0.0f;
        mMineralsDelta = 0.0f;
        mUncollectedTaxes = 0.0f;
        mBuildings.clear();
    }
}
