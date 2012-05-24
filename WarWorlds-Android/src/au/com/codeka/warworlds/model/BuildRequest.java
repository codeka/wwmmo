package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an in-progress build order.
 */
public class BuildRequest {
    private Logger log = LoggerFactory.getLogger(BuildRequest.class);
    private BuildKind mBuildKind;
    private String mDesignName;
    private String mColonyKey;
    private DateTime mEndTime;
    private DateTime mStartTime;
    private float mProgress;

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
    public float getProgress() {
        return mProgress;
    }
    public float getPercentComplete() {
        float percent = mProgress * 100.0f;
        if (percent < 0)
            percent = 0;
        if (percent > 100)
            percent = 100;
        return percent;
    }
    public Duration getRemainingTime() {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        log.info("getRemainingTime() now="+now+", endTime="+mEndTime);

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

    public static BuildRequest fromProtocolBuffer(warworlds.Warworlds.BuildRequest pb) {
        BuildRequest request = new BuildRequest();
        request.mBuildKind = BuildKind.fromNumber(pb.getBuildKind().getNumber());
        request.mDesignName = pb.getDesignName();
        request.mColonyKey = pb.getColonyKey();
        request.mEndTime = new DateTime(pb.getEndTime() * 1000, DateTimeZone.UTC);
        request.mStartTime = new DateTime(pb.getStartTime() * 1000, DateTimeZone.UTC);
        request.mProgress = pb.getProgress();
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
