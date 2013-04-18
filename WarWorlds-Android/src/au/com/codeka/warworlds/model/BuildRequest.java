package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.DesignKind;

/**
 * Represents an in-progress build order.
 */
public class BuildRequest extends BaseBuildRequest implements Parcelable {
    public void setEndTime(DateTime dt) {
        mEndTime = dt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeInt(mDesignKind.getValue());
        parcel.writeString(mDesignID);
        parcel.writeString(mColonyKey);
        parcel.writeLong(mEndTime.getMillis());
        parcel.writeLong(mStartTime.getMillis());
        parcel.writeLong(mRefreshTime.getMillis());
        parcel.writeFloat(mProgress);
        parcel.writeInt(mCount);
        parcel.writeString(mExistingBuildingKey);
        parcel.writeInt(mExistingBuildingLevel);
        parcel.writeString(mStarKey);
        parcel.writeInt(mPlanetIndex);
    }

    public static final Parcelable.Creator<BuildRequest> CREATOR
                = new Parcelable.Creator<BuildRequest>() {
        @Override
        public BuildRequest createFromParcel(Parcel parcel) {
            BuildRequest br = new BuildRequest();
            br.mKey = parcel.readString();
            br.mDesignKind = DesignKind.fromNumber(parcel.readInt());
            br.mDesignID = parcel.readString();
            br.mColonyKey = parcel.readString();
            br.mEndTime = new DateTime(parcel.readLong(), DateTimeZone.UTC);
            br.mStartTime = new DateTime(parcel.readLong(), DateTimeZone.UTC);
            br.mRefreshTime = new DateTime(parcel.readLong(), DateTimeZone.UTC);
            br.mProgress = parcel.readFloat();
            br.mCount = parcel.readInt();
            br.mExistingBuildingKey = parcel.readString();
            br.mExistingBuildingLevel = parcel.readInt();
            br.mStarKey = parcel.readString();
            br.mPlanetIndex = parcel.readInt();
            return br;
        }

        @Override
        public BuildRequest[] newArray(int size) {
            return new BuildRequest[size];
        }
    };
}
