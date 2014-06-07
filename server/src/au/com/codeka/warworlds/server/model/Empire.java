package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

public class Empire extends BaseEmpire {
    private int mID;
    private int mHomeStarID;
    private Integer mAllianceID;
    private boolean mForceRemoveAds;
    private DateTime mLastSitrepReadTime;

    public Empire() {
    }
    public Empire(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mKey = Integer.toString(mID);
        mDisplayName = res.getString("name");
        mCash = res.getFloat("cash");
        mEmailAddr = res.getString("user_email");
        mHomeStarID = res.getInt("home_star_id");
        mAllianceID = res.getInt("alliance_id");
        if (mAllianceID != null) {
            mAlliance = new Alliance(mAllianceID, res);
        }
        mState = State.fromNumber(res.getInt("state"));
        if (res.getInt("rank") != null) {
            mRank = new EmpireRank(res);
        }
        mLastSitrepReadTime = res.getDateTime("last_sitrep_read_time");
        mForceRemoveAds = res.getInt("remove_ads") != null && res.getInt("remove_ads") != 0;
        mShieldLastUpdate = res.getDateTime("shield_last_update");
    }

    public int getID() {
        return mID;
    }
    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(id);
    }

    public int getHomeStarID() {
        return mHomeStarID;
    }
    public void setHomeStar(Star star) {
        mHomeStar = star;
    }
    public void setName(String name) {
        mDisplayName = name.trim();
    }

    /**
     * Gets a value which indicates whether the flag in the database has been set that will
     * force ads to be removed from the game. To be used when people complain of ads coming back.
     */
    public boolean getForceRemoveAds() {
        return mForceRemoveAds;
    }

    public DateTime getLastSitrepReadTime() {
        return mLastSitrepReadTime;
    }

    @Override
    protected BaseEmpireRank createEmpireRank(Messages.EmpireRank pb) {
        EmpireRank er = new EmpireRank();
        if (pb != null) {
            er.fromProtocolBuffer(pb);
        }
        return er;
    }

    @Override
    protected BaseStar createStar(Messages.Star pb) {
        Star s = new Star();
        if (pb != null) {
            s.fromProtocolBuffer(pb);
        }
        return s;
    }

    @Override
    protected BaseAlliance createAlliance(Messages.Alliance pb) {
        Alliance a = new Alliance();
        if (pb != null) {
            a.fromProtocolBuffer(pb);
        }
        return a;
    }

}
