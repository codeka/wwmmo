package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseFleet;

public class Fleet extends BaseFleet implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mEmpireKey);
        parcel.writeString(mDesignID);
        parcel.writeFloat(mNumShips);
        parcel.writeInt(mState.getValue());
        parcel.writeLong(mStateStartTime.getMillis());
        parcel.writeString(mStarKey);
        parcel.writeString(mDestinationStarKey);
        parcel.writeString(mTargetFleetKey);
        parcel.writeString(mTargetColonyKey);
        parcel.writeInt(mStance.getValue());
    }

    public static final Parcelable.Creator<Fleet> CREATOR
                = new Parcelable.Creator<Fleet>() {
        @Override
        public Fleet createFromParcel(Parcel parcel) {
            Fleet f = new Fleet();
            f.mKey = parcel.readString();
            f.mEmpireKey = parcel.readString();
            f.mDesignID = parcel.readString();
            f.mNumShips = parcel.readFloat();
            f.mState = BaseFleet.State.fromNumber(parcel.readInt());
            f.mStateStartTime = new DateTime(parcel.readLong(), DateTimeZone.UTC);
            f.mStarKey = parcel.readString();
            f.mDestinationStarKey = parcel.readString();
            f.mTargetFleetKey = parcel.readString();
            f.mTargetColonyKey = parcel.readString();
            f.mStance = BaseFleet.Stance.fromNumber(parcel.readInt());
            return f;
        }

        @Override
        public Fleet[] newArray(int size) {
            return new Fleet[size];
        }
    };
}
