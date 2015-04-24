package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;

import au.com.codeka.common.protobuf.BuildRequest;

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
    protected Integer mExistingFleetID;
    protected String mUpgradeID;
    protected String mNotes;

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
    public Integer getExistingFleetID() {
        return mExistingFleetID;
    }
    public String getUpgradeID() {
        return mUpgradeID;
    }
    public String getNotes() {
        return mNotes;
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
        if (percentRemaining < 0) {
            percentRemaining = 0.0f;
        }
        if (percentRemaining > 1) {
            percentRemaining = 1.0f;
        }
        if (Float.isNaN(percentRemaining)) {
            // shouldn't get here...
            percentRemaining = 0.0f;
        }

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

    public void fromProtocolBuffer(BuildRequest pb) {
        mKey = pb.key;
        mDesignKind = DesignKind.fromNumber(pb.build_kind.getValue());
        mDesignID = pb.design_name;
        mColonyKey = pb.colony_key;
        mEndTime = new DateTime(pb.end_time * 1000, DateTimeZone.UTC);
        mStartTime = new DateTime(pb.start_time * 1000, DateTimeZone.UTC);
        mProgress = pb.progress;
        mCount = pb.count;
        mStarKey = pb.star_key;
        mPlanetIndex = pb.planet_index;
        mEmpireKey = pb.empire_key;
        if (pb.existing_building_key != null && !pb.existing_building_key.equals("")) {
            mExistingBuildingKey = pb.existing_building_key;
            mExistingBuildingLevel = pb.existing_building_level;
        }
        if (pb.existing_fleet_id != null && pb.upgrade_id != null) {
            mExistingFleetID = pb.existing_fleet_id;
            mUpgradeID = pb.upgrade_id;
        }
        mNotes = pb.notes;
    }

    public void toProtocolBuffer(BuildRequest pb) {
        pb.key = mKey;
        pb.build_kind = BuildRequest.BUILD_KIND.valueOf(mDesignKind.toString());
        pb.design_name = mDesignID;
        pb.colony_key = mColonyKey;
        pb.end_time = mEndTime.getMillis() / 1000;
        pb.start_time = mStartTime.getMillis() /1000;
        pb.progress = mProgress;
        pb.count = mCount;
        pb.star_key = mStarKey;
        pb.planet_index = mPlanetIndex;
        pb.empire_key = mEmpireKey;
        if (mExistingBuildingKey != null) {
            pb.existing_building_key = mExistingBuildingKey;
            pb.existing_building_level = mExistingBuildingLevel;
        }
        if (mExistingFleetID != null) {
            pb.existing_fleet_id = mExistingFleetID;
            pb.upgrade_id = mUpgradeID;
        }
        pb.notes = mNotes;
    }
}
