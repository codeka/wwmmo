package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;
import java.math.BigInteger;

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
            mTotalStars = BigInteger.valueOf(res.getLong("total_stars"));
            mTotalColonies = BigInteger.valueOf(res.getLong("total_colonies"));
            mTotalBuildings = BigInteger.valueOf(res.getLong("total_buildings"));
            mTotalShips = BigInteger.valueOf(res.getLong("total_ships"));
            mTotalPopulation = BigInteger.valueOf(res.getLong("total_population"));
        } catch (SQLException e) {
            // these may not exist... doesn't matter
        }
    }

    public int getEmpireID() {
        return mEmpireID;
    }

    public void setTotalShips(BigInteger num) {
        mTotalShips = num;
    }
    public void setTotalBuildings(BigInteger num) {
        mTotalBuildings = num;
    }
    public void setTotalColonies(BigInteger num) {
        mTotalColonies = num;
    }
    public void setTotalStars(BigInteger num) {
        mTotalStars = num;
    }
    public void setTotalPopulation(BigInteger num) {
        mTotalPopulation = num;
    }
}
