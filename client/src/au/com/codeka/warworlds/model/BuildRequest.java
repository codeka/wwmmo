package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.DesignKind;

/**
 * Represents an in-progress build order.
 */
public class BuildRequest extends BaseBuildRequest {
    public BuildRequest() {
    }
    public BuildRequest(String key, DesignKind designKind, String designID, String colonyKey,
                        DateTime startTime, int count, String existingBuildKey,
                        int existingBuildLevel, String starKey, int planetIndex,
                        String empireKey) {
        mKey = key;
        mDesignKind = designKind;
        mDesignID = designID;
        mColonyKey = colonyKey;
        mEndTime = startTime.plusHours(1);
        mStartTime = startTime;
        mRefreshTime = startTime;
        mProgress = 0.0f;
        mCount = count;
        mExistingBuildingKey = existingBuildKey;
        mExistingBuildingLevel = existingBuildLevel;
        mStarKey = starKey;
        mPlanetIndex = planetIndex;
        mEmpireKey = empireKey;
    }

    public void setEndTime(DateTime dt) {
        mEndTime = dt;
    }
}
