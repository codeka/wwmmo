package au.com.codeka.warworlds.server.model;

import au.com.codeka.common.model.BaseCombatReport;

public class CombatReport extends BaseCombatReport {
    private int mID;
    private int mStarID;

    public void setStarID(int id) {
        mStarID = id;
        mStarKey = Integer.toString(id);
    }
}
