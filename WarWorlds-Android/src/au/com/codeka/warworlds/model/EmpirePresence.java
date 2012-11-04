package au.com.codeka.warworlds.model;

import au.com.codeka.warworlds.model.protobuf.Messages;

/**
 * The \c EmpirePresence stores the data that relates to an empire's "presence" in a starsystem.
 * This includes things like 
 * @author dean@codeka.com.au
 *
 */
public class EmpirePresence {
    private String mKey;
    private String mEmpireKey;
    private String mStarKey;
    private float mTotalGoods;
    private float mTotalMinerals;
    private float mDeltaGoodsPerHour;
    private float mDeltaMineralsPerHour;

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
    public float getTotalMinerals() {
        return mTotalMinerals;
    }
    public float getDeltaGoodsPerHour() {
        return mDeltaGoodsPerHour;
    }
    public float getDeltaMineralsPerHour() {
        return mDeltaMineralsPerHour;
    }

    public static EmpirePresence fromProtocolBuffer(Messages.EmpirePresence pb) {
        EmpirePresence presence = new EmpirePresence();
        presence.mKey = pb.getKey();
        presence.mEmpireKey = pb.getEmpireKey();
        presence.mStarKey = pb.getStarKey();
        presence.mTotalGoods = pb.getTotalGoods();
        presence.mTotalMinerals = pb.getTotalMinerals();
        presence.mDeltaGoodsPerHour = pb.getGoodsDeltaPerHour();
        presence.mDeltaMineralsPerHour = pb.getMineralsDeltaPerHour();
        return presence;
    }
}
