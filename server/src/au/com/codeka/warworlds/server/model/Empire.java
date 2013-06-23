package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;

public class Empire extends BaseEmpire {
    private int mID;
    private int mHomeStarID;
    private int mAllianceID;
    private boolean mForceRemoveAds;

    public Empire() {
    }
    public Empire(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mKey = Integer.toString(mID);
        mDisplayName = rs.getString("name");
        mCash = rs.getFloat("cash");
        mEmailAddr = rs.getString("user_email");
        mHomeStarID = rs.getInt("home_star_id");
        mAllianceID = rs.getInt("alliance_id");
        if (!rs.wasNull()) {
            mAlliance = new Alliance(mAllianceID, rs);
        }

        rs.getInt("rank");
        if (!rs.wasNull()) {
            mRank = new EmpireRank(rs);
        }

        mForceRemoveAds = rs.getInt("remove_ads") != 0;
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

    /**
     * Gets a value which indicates whether the flag in the database has been set that will
     * force ads to be removed from the game. To be used when people complain of ads coming back.
     */
    public boolean getForceRemoveAds() {
        return mForceRemoveAds;
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
