package au.com.codeka.warworlds.server.model;

import au.com.codeka.common.model.BaseCombatReport;

public class CombatReport extends BaseCombatReport {
    private int mID;
    private int mStarID;

    public int getID() {
        return mID;
    }
    public int getStarID() {
        return mStarID;
    }

    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(id);
    }
    public void setStarID(int id) {
        mStarID = id;
        mStarKey = Integer.toString(id);
    }
}
