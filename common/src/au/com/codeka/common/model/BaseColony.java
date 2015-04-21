package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Colony;

public abstract class BaseColony {
    protected String mKey;
    protected String mStarKey;
    protected int mPlanetIndex;
    protected float mPopulation;
    protected String mEmpireKey;
    protected float mFarmingFocus;
    protected float mConstructionFocus;
    protected float mPopulationFocus;
    protected float mMiningFocus;
    protected float mPopulationDelta;
    protected float mGoodsDelta;
    protected float mMineralsDelta;
    protected float mUncollectedTaxes;
    protected List<BaseBuilding> mBuildings;
    protected float mMaxPopulation;
    protected float mDefenceBoost;
    protected DateTime mCooldownTimeEnd;

    public String getKey() {
        return mKey;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public int getPlanetIndex() {
        return mPlanetIndex;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public float getPopulation() {
        if (mPopulation <= 0.0f) {
            return 0.0f;
        }
        return mPopulation;
    }
    public void setPopulation(float pop) {
        mPopulation = pop;
    }
    public float getFarmingFocus() {
        return mFarmingFocus;
    }
    public void setFarmingFocus(float focus) {
        mFarmingFocus = focus;
    }
    public float getConstructionFocus() {
        return mConstructionFocus;
    }
    public void setConstructionFocus(float focus) {
        mConstructionFocus = focus;
    }
    public float getPopulationFocus() {
        return mPopulationFocus;
    }
    public void setPopulationFocus(float focus) {
        mPopulationFocus = focus;
    }
    public float getMiningFocus() {
        return mMiningFocus;
    }
    public void setMiningFocus(float focus) {
        mMiningFocus = focus;
    }
    public float getPopulationDelta() {
        return mPopulationDelta;
    }
    public void setPopulationDelta(float dp) {
        mPopulationDelta = dp;
    }
    public float getGoodsDelta() {
        return mGoodsDelta;
    }
    public void setGoodsDelta(float d) {
        mGoodsDelta = d;
    }
    public float getMineralsDelta() {
        return mMineralsDelta;
    }
    public void setMineralsDelta(float d) {
        mMineralsDelta = d;
    }
    public float getUncollectedTaxes() {
        return mUncollectedTaxes;
    }
    public void setUncollectedTaxes(float taxes) {
        mUncollectedTaxes = taxes;
    }
    public List<BaseBuilding> getBuildings() {
        return mBuildings;
    }
    public float getMaxPopulation() {
        return mMaxPopulation;
    }
    public float getDefenceBoost() {
        return mDefenceBoost;
    }
    public boolean isInCooldown() {
        if (mCooldownTimeEnd == null) {
            return false;
        }
        DateTime now = DateTime.now(DateTimeZone.UTC);
        return (now.compareTo(mCooldownTimeEnd) < 0);
    }
    public DateTime getCooldownEndTime() {
        return mCooldownTimeEnd;
    }

    public void fromProtocolBuffer(Colony pb) {
        mKey = pb.key;
        mStarKey = pb.star_key;
        mPlanetIndex = pb.planet_index;
        mPopulation = pb.population;
        mEmpireKey = pb.empire_key;
        mBuildings = new ArrayList<>();
        mFarmingFocus = pb.focus_farming;
        mConstructionFocus = pb.focus_construction;
        mMiningFocus = pb.focus_mining;
        mPopulationFocus = pb.focus_population;
        mPopulationDelta = pb.delta_population;
        mGoodsDelta = pb.delta_goods;
        mMineralsDelta = pb.delta_minerals;
        mUncollectedTaxes = pb.uncollected_taxes;
        mMaxPopulation = pb.max_population;
        mDefenceBoost = pb.defence_bonus;
        if (pb.cooldown_end_time != null) {
            mCooldownTimeEnd = new DateTime(pb.cooldown_end_time * 1000, DateTimeZone.UTC);
        }
    }

    public void toProtocolBuffer(Colony.Builder pb) {
        pb.key = mKey;
        pb.planet_index = mPlanetIndex;
        pb.star_key = mStarKey;
        pb.empire_key = mEmpireKey;
        pb.population = mPopulation;
        pb.focus_population = mPopulationFocus;
        pb.focus_farming = mFarmingFocus;
        pb.focus_mining = mMiningFocus;
        pb.focus_construction = mConstructionFocus;
        pb.delta_minerals = mMineralsDelta;
        pb.delta_goods = mGoodsDelta;
        pb.delta_population = mPopulationDelta;
        pb.max_population = mMaxPopulation;
        pb.uncollected_taxes = mUncollectedTaxes;
        pb.defence_bonus = mDefenceBoost;
        if (mCooldownTimeEnd != null) {
            pb.cooldown_end_time = mCooldownTimeEnd.getMillis() / 1000;
        }
    }
}
