package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.warworlds.server.data.SqlResult;

public class EmpirePresence extends BaseEmpirePresence {
    private int mID;
    private int mStarID;
    private int mEmpireID;

    public EmpirePresence() {
    }
    public EmpirePresence(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mKey = Integer.toString(mID);
        mStarID = res.getInt("star_id");
        mStarKey = Integer.toString(mStarID);
        mEmpireID = res.getInt("empire_id");
        mEmpireKey = Integer.toString(mEmpireID);
        mTotalGoods = res.getFloat("total_goods");
        mTotalMinerals = res.getFloat("total_minerals");
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
