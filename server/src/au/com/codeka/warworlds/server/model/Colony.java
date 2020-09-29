package au.com.codeka.warworlds.server.model;

import com.google.api.client.util.Objects;

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
    mBuildings = new ArrayList<>();
  }

  public Colony(int id, int sectorID, int starID, int planetIndex, Integer empireID,
                float population, float focusPopulation, float focusFarming, float focusMining,
                float focusConstruction) {
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

    // Make sure the focuses add up to 1.0.
    float total = focusPopulation + focusFarming + focusMining + focusConstruction;
    if (total < 0.1f) {
      focusPopulation = 0.25f;
      focusFarming = 0.25f;
      focusMining = 0.25f;
      focusConstruction = 0.25f;
    } else {
      focusPopulation *= 1.0f / total;
      focusFarming *= 1.0f / total;
      focusMining *= 1.0f / total;
      focusConstruction *= 1.0f / total;
    }

    mPopulationFocus = focusPopulation;
    mConstructionFocus = focusConstruction;
    mFarmingFocus = focusFarming;
    mMiningFocus = focusMining;
    mUncollectedTaxes = 0.0f;
    mCooldownTimeEnd = new DateTime().plusHours(8);
    mPopulation = population;
    mDefenceBoost = 1.0f;
    mBuildings = new ArrayList<>();
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
   * Set the colony to abandoned. Make sure the focus is reasonable for a native colony and set the
   * empire ID to null.
   */
  public void setAbandoned() {
    mEmpireID = null;
    mEmpireKey = null;
    mPopulationFocus = 0.1f;
    mFarmingFocus = 0.99f;
    mMiningFocus = 0.0f;
    mConstructionFocus = 0.0f;
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

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("id", mID)
        .add("planetIndex", mPlanetIndex)
        .add("empireID", mEmpireID)
        .add("populationFocus", mPopulationFocus)
        .add("constructionFocus", mConstructionFocus)
        .add("miningFocus", mMiningFocus)
        .add("uncollectedTaxes", mUncollectedTaxes)
        .add("cooldownTimeEnd", mCooldownTimeEnd)
        .add("population", mPopulation)
        .add("defenseBoost", mDefenceBoost)
        .add("buildings", mBuildings)
        .toString();
  }
}
