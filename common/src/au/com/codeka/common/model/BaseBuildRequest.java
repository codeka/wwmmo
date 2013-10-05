package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;

import au.com.codeka.common.protobuf.Messages;

/**
 * Represents an in-progress build order.
 */
public abstract class BaseBuildRequest  {
    protected String mKey;
    protected DesignKind mDesignKind;
    protected String mDesignID;
    protected String mColonyKey;
    protected DateTime mEndTime;
    protected DateTime mStartTime;
    protected DateTime mRefreshTime;
    protected float mProgress;
    protected int mCount;
    protected String mExistingBuildingKey;
    protected int mExistingBuildingLevel;
    protected String mStarKey;
    protected int mPlanetIndex;
    protected String mEmpireKey;

    public BaseBuildRequest() {
        mRefreshTime = DateTime.now(DateTimeZone.UTC);
    }

    public String getKey() {
        return mKey;
    }
    public DesignKind getDesignKind() {
        return mDesignKind;
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
    public int getExistingBuildingLevel() {
        return mExistingBuildingLevel;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public int getPlanetIndex() {
        return mPlanetIndex;
    }
    public String getEmpireKey() {
        return mEmpireKey;
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
        if (mRefreshTime.isAfter(now)) {
            return mProgress;
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

    public void setStartTime(DateTime dt) {
        mStartTime = dt;
    }
    public void setEndTime(DateTime dt) {
        mEndTime = dt;
    }

    public void fromProtocolBuffer(Messages.BuildRequest pb) {
        if (pb.hasKey()) {
            mKey = pb.getKey();
        }
        mDesignKind = DesignKind.fromNumber(pb.getBuildKind().getNumber());
        mDesignID = pb.getDesignName();
        mColonyKey = pb.getColonyKey();
        mEndTime = new DateTime(pb.getEndTime() * 1000, DateTimeZone.UTC);
        mStartTime = new DateTime(pb.getStartTime() * 1000, DateTimeZone.UTC);
        mProgress = pb.getProgress();
        mCount = pb.getCount();
        mStarKey = pb.getStarKey();
        mPlanetIndex = pb.getPlanetIndex();
        mEmpireKey = pb.getEmpireKey();
        if (pb.getExistingBuildingKey() != null && !pb.getExistingBuildingKey().equals("")) {
            mExistingBuildingKey = pb.getExistingBuildingKey();
            mExistingBuildingLevel = pb.getExistingBuildingLevel(); 
        }
    }

    public void toProtocolBuffer(Messages.BuildRequest.Builder pb) {
        if (mKey != null) {
            pb.setKey(mKey);
        }
        pb.setBuildKind(Messages.BuildRequest.BUILD_KIND.valueOf(mDesignKind.getValue()));
        pb.setDesignName(mDesignID);
        pb.setColonyKey(mColonyKey);
        pb.setEndTime(mEndTime.getMillis() / 1000);
        pb.setStartTime(mStartTime.getMillis() /1000);
        pb.setProgress(mProgress);
        pb.setCount(mCount);
        pb.setStarKey(mStarKey);
        pb.setPlanetIndex(mPlanetIndex);
        pb.setEmpireKey(mEmpireKey);
        if (mExistingBuildingKey != null) {
            pb.setExistingBuildingKey(mExistingBuildingKey);
            pb.setExistingBuildingLevel(mExistingBuildingLevel);
        }
    }
}
