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
            mTotalStars = res.getInt("total_stars");
            mTotalColonies = res.getInt("total_colonies");
            mTotalBuildings = res.getInt("total_buildings");
            mTotalShips = res.getInt("total_ships");
            mTotalPopulation = res.getInt("total_population");
        } catch (SQLException e) {
            // these may not exist... doesn't matter
        }
    }

    public int getEmpireID() {
        return mEmpireID;
    }

    public void setTotalShips(int num) {
        mTotalShips = num;
    }
    public void setTotalBuildings(int num) {
        mTotalBuildings = num;
    }
    public void setTotalColonies(int num) {
        mTotalColonies = num;
    }
    public void setTotalStars(int num) {
        mTotalStars = num;
    }
    public void setTotalPopulation(int num) {
        mTotalPopulation = num;
    }
}
