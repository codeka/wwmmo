package au.com.codeka.warworlds.model;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseEmpirePresence;

/**
 * The \c EmpirePresence stores the data that relates to an empire's "presence" in a starsystem.
 * This includes things like 
 * @author dean@codeka.com.au
 *
 */
public class EmpirePresence extends BaseEmpirePresence implements Parcelable {
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
