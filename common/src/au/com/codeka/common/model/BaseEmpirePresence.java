package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

/**
 * The \c EmpirePresence stores the data that relates to an empire's "presence" in a starsystem.
 * This includes things like 
 * @author dean@codeka.com.au
 *
 */
public abstract class BaseEmpirePresence {
    protected String mKey;
    protected String mEmpireKey;
    protected String mStarKey;
    protected float mTotalGoods;
    protected float mTotalMinerals;
    protected float mDeltaGoodsPerHour;
    protected float mDeltaMineralsPerHour;
    protected float mMaxGoods;
    protected float mMaxMinerals;

    public String getKey() {
        return mKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public float getTotalGoods() {
        return mTotalGoods;
    }
    public void setTotalGoods(float goods) {
        mTotalGoods = goods;
    }
    public float getTotalMinerals() {
        return mTotalMinerals;
    }
    public void setTotalMinerals(float minerals) {
        mTotalMinerals = minerals;
    }
    public float getDeltaGoodsPerHour() {
        return mDeltaGoodsPerHour;
    }
    public void setDeltaGoodsPerHour(float d) {
        mDeltaGoodsPerHour = d;
    }
    public float getDeltaMineralsPerHour() {
        return mDeltaMineralsPerHour;
    }
    public void setDeltaMineralsPerHour(float d) {
        mDeltaMineralsPerHour = d;
    }
    public float getMaxGoods() {
        return mMaxGoods;
    }
    public void setMaxGoods(float value) {
        mMaxGoods = value;
    }
    public float getMaxMinerals() {
        return mMaxMinerals;
    }
    public void setMaxMinerals(float value) {
        mMaxMinerals = value;
    }

    public void fromProtocolBuffer(Messages.EmpirePresence pb) {
        mKey = pb.getKey();
        mEmpireKey = pb.getEmpireKey();
        mStarKey = pb.getStarKey();
        mTotalGoods = pb.getTotalGoods();
        mTotalMinerals = pb.getTotalMinerals();
        mDeltaGoodsPerHour = pb.getGoodsDeltaPerHour();
        mDeltaMineralsPerHour = pb.getMineralsDeltaPerHour();
        mMaxGoods = pb.getMaxGoods();
        mMaxMinerals = pb.getMaxMinerals();
    }

    public void toProtocolBuffer(Messages.EmpirePresence.Builder pb) {
        pb.setKey(mKey);
        pb.setEmpireKey(mEmpireKey);
        pb.setStarKey(mStarKey);
        pb.setTotalGoods(mTotalGoods);
        pb.setTotalMinerals(mTotalMinerals);
        pb.setGoodsDeltaPerHour(mDeltaGoodsPerHour);
        pb.setMineralsDeltaPerHour(mDeltaMineralsPerHour);
        pb.setMaxGoods(mMaxGoods);
        pb.setMaxMinerals(mMaxMinerals);
    }
}
