package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import au.com.codeka.common.model.BaseEmpirePresence;

public class EmpirePresence extends BaseEmpirePresence {
    private int mID;
    private int mStarID;
    private int mEmpireID;

    public EmpirePresence() {
    }
    public EmpirePresence(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mKey = Integer.toString(mID);
        mStarID = rs.getInt("star_id");
        mStarKey = Integer.toString(mStarID);
        mEmpireID = rs.getInt("empire_id");
        mEmpireKey = Integer.toString(mEmpireID);
        mTotalGoods = rs.getFloat("total_goods");
        mTotalMinerals = rs.getFloat("total_minerals");
    }

    public int getID() {
        return mID;
    }
    public int getStarID() {
        return mStarID;
    }
    public int getEmpireID() {
        return mEmpireID;
    }
}
