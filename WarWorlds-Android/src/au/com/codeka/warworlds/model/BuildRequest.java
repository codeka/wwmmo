package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;

/**
 * Represents an in-progress build order.
 */
public class BuildRequest implements Parcelable {
    private String mKey;
    private BuildKind mBuildKind;
    private String mDesignID;
    private String mColonyKey;
    private DateTime mEndTime;
    private DateTime mStartTime;
    private DateTime mRefreshTime;
    private float mProgress;
    private int mCount;
    private String mExistingBuildingKey;

    public BuildRequest() {
        mRefreshTime = DateTime.now(DateTimeZone.UTC);
    }

    public String getKey() {
        return mKey;
    }
    public BuildKind getBuildKind() {
        return mBuildKind;
    }
    public String getDesignID() {
        return mDesignID;
    }
    public DateTime getStartTime() {
        return mStartTime;
    }
    public DateTime getEndTime() {
        return mEndTime;
    }
    public int getCount() {
        return mCount;
    }
    public String getExistingBuildingKey() {
        return mExistingBuildingKey;
    }
    public float getProgress(boolean interpolate) {
        if (!interpolate) {
            return mProgress;
        }

        // mProgress will be the accurate at the time this BuildRequest was refreshed from the
        // server. We'll do a little bit of interpolation so that it's a good estimate *after*
        // we've been refreshed from the server, too.
        DateTime now = DateTime.now(DateTimeZone.UTC);
        if (mEndTime.isBefore(now)) {
            return 1.0f;
        }

        long numerator = new Interval(mRefreshTime, now).toDurationMillis();
        long denominator = new Interval(mRefreshTime, mEndTime).toDurationMillis();
        float percentRemaining = (float) numerator / (float) denominator;

        return mProgress + ((1.0f - mProgress) * percentRemaining);
    }
    public void setProgress(float progress) {
        mProgress = progress;
    }
    public float getPercentComplete() {
        float percent = getProgress(true) * 100.0f;
        if (percent < 0)
            percent = 0;
        if (percent > 100)
            percent = 100;
        return percent;
    }
    public Duration getRemainingTime() {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        if (mEndTime.isBefore(now)) {
            return Duration.ZERO;
        }

        Duration d = new Interval(now, mEndTime).toDuration();
        // if it's actually zero, add a few seconds (to differentiate between "REALLY now" and
        // "it'll never finish, so lets return zero")
        if (d.compareTo(Duration.standardSeconds(5)) <  0)
            return Duration.standardSeconds(5);
        return d;
    }
    public String getColonyKey() {
        return mColonyKey;
    }

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
        parcel.writeInt(mBuildKind.getValue());
        parcel.writeString(mDesignID);
        parcel.writeString(mColonyKey);
        parcel.writeLong(mEndTime.getMillis());
        parcel.writeLong(mStartTime.getMillis());
        parcel.writeLong(mRefreshTime.getMillis());
        parcel.writeFloat(mProgress);
        parcel.writeInt(mCount);
        parcel.writeString(mExistingBuildingKey);
    }

    public static final Parcelable.Creator<BuildRequest> CREATOR
                = new Parcelable.Creator<BuildRequest>() {
        @Override
        public BuildRequest createFromParcel(Parcel parcel) {
            BuildRequest br = new BuildRequest();
            br.mKey = parcel.readString();
            br.mBuildKind = BuildKind.fromNumber(parcel.readInt());
            br.mDesignID = parcel.readString();
            br.mColonyKey = parcel.readString();
            br.mEndTime = new DateTime(parcel.readLong(), DateTimeZone.UTC);
            br.mStartTime = new DateTime(parcel.readLong(), DateTimeZone.UTC);
            br.mRefreshTime = new DateTime(parcel.readLong(), DateTimeZone.UTC);
            br.mProgress = parcel.readFloat();
            br.mCount = parcel.readInt();
            br.mExistingBuildingKey = parcel.readString();
            return br;
        }

        @Override
        public BuildRequest[] newArray(int size) {
            return new BuildRequest[size];
        }
    };

    public static BuildRequest fromProtocolBuffer(Messages.BuildRequest pb) {
        BuildRequest request = new BuildRequest();
        request.mKey = pb.getKey();
        request.mBuildKind = BuildKind.fromNumber(pb.getBuildKind().getNumber());
        request.mDesignID = pb.getDesignName();
        request.mColonyKey = pb.getColonyKey();
        request.mEndTime = new DateTime(pb.getEndTime() * 1000, DateTimeZone.UTC);
        request.mStartTime = new DateTime(pb.getStartTime() * 1000, DateTimeZone.UTC);
        request.mProgress = pb.getProgress();
        request.mCount = pb.getCount();
        if (pb.getExistingBuildingKey() != null && !pb.getExistingBuildingKey().equals("")) {
            request.mExistingBuildingKey = pb.getExistingBuildingKey();
        }
        return request;
    }

    // The value of BuildKind needs to be kept in sync with WarWorlds.proto's BUILD_KIND enum.
    public enum BuildKind {
        BUILDING(1),
        SHIP(2);

        private int mValue;

        BuildKind(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static BuildKind fromNumber(int value) {
            for(BuildKind bk : BuildKind.values()) {
                if (bk.getValue() == value) {
                    return bk;
                }
            }

            return BuildKind.BUILDING;

        }
    }
}
