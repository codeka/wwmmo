package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.warworlds.server.data.SqlResult;

public class EmpireRank extends BaseEmpireRank {
    private int mEmpireID;

    public EmpireRank() {
    }
    public EmpireRank(SqlResult res) throws SQLException {
        mEmpireID = res.getInt("empire_id");
        mEmpireKey = Integer.toString(mEmpireID);

        try {
            mRank = res.getInt("rank");
            mTotalStars = res.getLong("total_stars");
            mTotalColonies = res.getLong("total_colonies");
            mTotalBuildings = res.getLong("total_buildings");
            mTotalShips = res.getLong("total_ships");
            mTotalPopulation = res.getLong("total_population");
        } catch (SQLException e) {
            // these may not exist... doesn't matter
        }
    }

    public int getEmpireID() {
        return mEmpireID;
    }

    public void setTotalShips(long num) {
        mTotalShips = num;
    }
    public void setTotalBuildings(long num) {
        mTotalBuildings = num;
    }
    public void setTotalColonies(long num) {
        mTotalColonies = num;
    }
    public void setTotalStars(long num) {
        mTotalStars = num;
    }
    public void setTotalPopulation(long num) {
        mTotalPopulation = num;
    }
}
