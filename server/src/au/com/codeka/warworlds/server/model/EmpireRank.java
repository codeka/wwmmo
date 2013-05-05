package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import au.com.codeka.common.model.BaseEmpireRank;

public class EmpireRank extends BaseEmpireRank {
    private int mEmpireID;

    public EmpireRank() {
    }
    public EmpireRank(ResultSet rs) throws SQLException {
        mEmpireID = rs.getInt("id");
        mEmpireKey = Integer.toString(mEmpireID);
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
}
