package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import au.com.codeka.common.model.BaseEmpireRank;

public class EmpireRank extends BaseEmpireRank {
    private int mEmpireID;

    public EmpireRank() {
    }
    public EmpireRank(ResultSet rs) throws SQLException {
        mEmpireID = rs.getInt("empire_id");
        mEmpireKey = Integer.toString(mEmpireID);

        try {
            mRank = rs.getInt("rank");
            mTotalStars = rs.getInt("total_stars");
            mTotalColonies = rs.getInt("total_colonies");
            mTotalBuildings = rs.getInt("total_buildings");
            mTotalShips = rs.getInt("total_ships");
            mTotalPopulation = rs.getInt("total_population");
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
