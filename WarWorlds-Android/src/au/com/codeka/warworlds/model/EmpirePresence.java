package au.com.codeka.warworlds.model;

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

    public static EmpirePresence fromProtocolBuffer(warworlds.Warworlds.EmpirePresence pb) {
        EmpirePresence presence = new EmpirePresence();
        presence.mKey = pb.getKey();
        presence.mEmpireKey = pb.getEmpireKey();
        presence.mStarKey = pb.getStarKey();
        presence.mTotalGoods = pb.getTotalGoods();
        presence.mTotalMinerals = pb.getTotalMinerals();
        return presence;
    }
}
