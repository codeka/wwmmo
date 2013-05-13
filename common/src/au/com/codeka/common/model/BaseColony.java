package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

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

    public void fromProtocolBuffer(Messages.Colony pb) {
        mKey = pb.getKey();
        mStarKey = pb.getStarKey();
        mPlanetIndex = pb.getPlanetIndex();
        mPopulation = pb.getPopulation();
        if (pb.hasEmpireKey()) {
            mEmpireKey = pb.getEmpireKey();
        }
        mBuildings = new ArrayList<BaseBuilding>();
        mFarmingFocus = pb.getFocusFarming();
        mConstructionFocus = pb.getFocusConstruction();
        mMiningFocus = pb.getFocusMining();
        mPopulationFocus = pb.getFocusPopulation();
        mPopulationDelta = pb.getDeltaPopulation();
        mGoodsDelta = pb.getDeltaGoods();
        mMineralsDelta = pb.getDeltaMinerals();
        mUncollectedTaxes = pb.getUncollectedTaxes();
        mMaxPopulation = pb.getMaxPopulation();
        mDefenceBoost = pb.getDefenceBonus();
        if (pb.hasCooldownEndTime()) {
            mCooldownTimeEnd = new DateTime(pb.getCooldownEndTime() * 1000, DateTimeZone.UTC);
        }
    }

    public void toProtocolBuffer(Messages.Colony.Builder pb) {
        pb.setKey(mKey);
        pb.setPlanetIndex(mPlanetIndex);
        pb.setStarKey(mStarKey);
        if (mEmpireKey != null) {
            pb.setEmpireKey(mEmpireKey);
        }
        pb.setPopulation(mPopulation);
        pb.setFocusPopulation(mPopulationFocus);
        pb.setFocusFarming(mFarmingFocus);
        pb.setFocusMining(mMiningFocus);
        pb.setFocusConstruction(mConstructionFocus);
        pb.setDeltaMinerals(mMineralsDelta);
        pb.setDeltaGoods(mGoodsDelta);
        pb.setDeltaPopulation(mPopulationDelta);
        pb.setMaxPopulation(mMaxPopulation);
        pb.setUncollectedTaxes(mUncollectedTaxes);
        pb.setDefenceBonus(mDefenceBoost);
        if (mCooldownTimeEnd != null) {
            pb.setCooldownEndTime(mCooldownTimeEnd.getMillis() / 1000);
        }
    }
}
