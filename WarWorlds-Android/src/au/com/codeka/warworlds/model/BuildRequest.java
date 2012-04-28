package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;

/**
 * Represents an in-progress build order.
 */
public class BuildRequest {
    private BuildKind mBuildKind;
    private String mDesignName;
    private String mColonyKey;
    private DateTime mEndTime;
    private DateTime mStartTime;

    public BuildKind getBuildKind() {
        return mBuildKind;
    }
    public String getDesignName() {
        return mDesignName;
    }
    public DateTime getStartTime() {
        return mStartTime;
    }
    public DateTime getEndTime() {
        return mEndTime;
    }
    public float getPercentComplete() {
        // TODO: this should be returned to us from the server....
        DateTime now = DateTime.now(DateTimeZone.UTC);

        if (now.isBefore(mStartTime) || now.equals(mStartTime)) {
            return 0.0f;
        }
        if (mEndTime.isBefore(now) || mEndTime.equals(now)) {
            return 100.0f;
        }

        long totalBuildTime = new Interval(mStartTime, mEndTime).toDurationMillis();
        long currentBuildComplete = new Interval(mStartTime, now).toDurationMillis();
        return 100.0f * (float) currentBuildComplete / (float) totalBuildTime;
    }
    public Duration getRemainingTime() {
        DateTime now = DateTime.now(DateTimeZone.UTC);

        if (mEndTime.isBefore(now)) {
            return Duration.ZERO;
        }
        return new Interval(now, mEndTime).toDuration();
    }
    public BuildingDesign getBuildingDesign() {
        if (mBuildKind != BuildKind.BUILDING) {
            throw new IllegalArgumentException("Cannot getBuildingDesign when BuildKind != BUILDING");
        }
        return BuildingDesignManager.getInstance().getDesign(mDesignName);
    }
    public String getColonyKey() {
        return mColonyKey;
    }

    public static BuildRequest fromProtocolBuffer(warworlds.Warworlds.BuildRequest pb) {
        BuildRequest request = new BuildRequest();
        request.mBuildKind = BuildKind.fromNumber(pb.getBuildKind().getNumber());
        request.mDesignName = pb.getDesignName();
        request.mColonyKey = pb.getColonyKey();
        request.mEndTime = new DateTime(pb.getEndTime() * 1000, DateTimeZone.UTC);
        request.mStartTime = new DateTime(pb.getStartTime() * 1000, DateTimeZone.UTC);
        return request;
    }

    public enum BuildKind {
        BUILDING(0),
        SHIP(1);

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
