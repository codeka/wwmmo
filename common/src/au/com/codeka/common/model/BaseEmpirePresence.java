package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.EmpirePresence;

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
    protected float mTaxPerHour;
    protected DateTime mGoodsZeroTime;

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
    public float getTaxPerHour() {
        return mTaxPerHour;
    }
    public void setTaxPerHour(float tax) {
        mTaxPerHour = tax;
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

    /**
     * Gets the \c DateTime the goods will drop to zero. We'll want to notify the player
     * that they need to pay attention to this star.
     */
    public DateTime getGoodsZeroTime() {
        return mGoodsZeroTime;
    }
    public void setGoodsZeroTime(DateTime dt) {
        mGoodsZeroTime = dt;
    }

    public void fromProtocolBuffer(EmpirePresence pb) {
        mKey = pb.key;
        mEmpireKey = pb.empire_key;
        mStarKey = pb.star_key;
        mTotalGoods = pb.total_goods;
        mTotalMinerals = pb.total_minerals;
        mDeltaGoodsPerHour = pb.goods_delta_per_hour;
        mDeltaMineralsPerHour = pb.minerals_delta_per_hour;
        mMaxGoods = pb.max_goods;
        mMaxMinerals = pb.max_minerals;
        if (pb.goods_zero_time != null) {
            mGoodsZeroTime = new DateTime(pb.goods_zero_time * 1000, DateTimeZone.UTC);
        }
    }

    public void toProtocolBuffer(EmpirePresence pb) {
        pb.key = mKey;
        pb.empire_key = mEmpireKey;
        pb.star_key = mStarKey;
        pb.total_goods = mTotalGoods;
        pb.total_minerals = mTotalMinerals;
        pb.goods_delta_per_hour = mDeltaGoodsPerHour;
        pb.minerals_delta_per_hour = mDeltaMineralsPerHour;
        pb.max_goods = mMaxGoods;
        pb.max_minerals = mMaxMinerals;
        if (mGoodsZeroTime != null) {
            pb.goods_zero_time = mGoodsZeroTime.getMillis() / 1000;
        }
    }
}
