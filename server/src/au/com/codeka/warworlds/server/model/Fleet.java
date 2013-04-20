package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseFleet;

public class Fleet extends BaseFleet {
    private int mID;
    private int mStarID;
    private int mSectorID;
    private int mEmpireID;
    private int mDestinationStarID;
    private int mTargetFleetID;

    public Fleet() {
    }
    public Fleet(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mKey = Integer.toString(mID);
        mStarID = rs.getInt("star_id");
        mStarKey = Integer.toString(mStarID);
        mSectorID = rs.getInt("sector_id");
        mDesignID = rs.getString("design_name");
        mEmpireID = rs.getInt("empire_id");
        if (!rs.wasNull()) {
            mEmpireKey = Integer.toString(mEmpireID);
        }
        mNumShips = rs.getFloat("num_ships");
        mStance = Stance.fromNumber(rs.getInt("stance"));
        mState = State.fromNumber(rs.getInt("state"));
        mStateStartTime = new DateTime(rs.getTimestamp("state_start_time").getTime());

        Timestamp eta = rs.getTimestamp("eta");
        if (eta != null) {
            mEta = new DateTime(eta.getTime());
        }

        mDestinationStarID = rs.getInt("target_star_id");
        if (!rs.wasNull()) {
            mDestinationStarKey = Integer.toString(mDestinationStarID);
        }

        mTargetFleetID = rs.getInt("target_fleet_id");
        if (!rs.wasNull()) {
            mTargetFleetKey = Integer.toString(mTargetFleetID);
        }
    }

    public int getStarID() {
        return mStarID;
    }
    public int getSectorID() {
        return mSectorID;
    }
    public int getEmpireID() {
        return mEmpireID;
    }
}
