package au.com.codeka.warworlds.model;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;

/**
 * The \c EmpirePresence stores the data that relates to an empire's "presence" in a starsystem.
 * This includes things like 
 * @author dean@codeka.com.au
 *
 */
public class EmpirePresence implements Parcelable {
    private String mKey;
    private String mEmpireKey;
    private String mStarKey;
    private float mTotalGoods;
    private float mTotalMinerals;
    private float mDeltaGoodsPerHour;
    private float mDeltaMineralsPerHour;
    private float mMaxGoods;
    private float mMaxMinerals;

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
    public float getMaxMinerals() {
        return mMaxMinerals;
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
        presence.mMaxGoods = pb.getMaxGoods();
        presence.mMaxMinerals = pb.getMaxMinerals();
        return presence;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mEmpireKey);
        parcel.writeString(mStarKey);
        parcel.writeFloat(mTotalGoods);
        parcel.writeFloat(mTotalMinerals);
        parcel.writeFloat(mDeltaGoodsPerHour);
        parcel.writeFloat(mDeltaMineralsPerHour);
        parcel.writeFloat(mMaxGoods);
        parcel.writeFloat(mMaxMinerals);
    }


    public static final Parcelable.Creator<EmpirePresence> CREATOR
                = new Parcelable.Creator<EmpirePresence>() {
        @Override
        public EmpirePresence createFromParcel(Parcel parcel) {
            EmpirePresence empire = new EmpirePresence();
            empire.mKey = parcel.readString();
            empire.mEmpireKey = parcel.readString();
            empire.mStarKey = parcel.readString();
            empire.mTotalGoods = parcel.readFloat();
            empire.mTotalMinerals = parcel.readFloat();
            empire.mDeltaGoodsPerHour = parcel.readFloat();
            empire.mDeltaMineralsPerHour= parcel.readFloat();
            empire.mMaxGoods = parcel.readFloat();
            empire.mMaxMinerals= parcel.readFloat();
            return empire;
        }

        @Override
        public EmpirePresence[] newArray(int size) {
            return new EmpirePresence[size];
        }
    };
}
