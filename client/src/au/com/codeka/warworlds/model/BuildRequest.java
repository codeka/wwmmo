package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseBuildRequest;

/**
 * Represents an in-progress build order.
 */
public class BuildRequest extends BaseBuildRequest {
    public void setEndTime(DateTime dt) {
        mEndTime = dt;
    }
}
