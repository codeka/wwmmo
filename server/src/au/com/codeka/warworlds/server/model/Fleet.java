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

    public int getID() {
        return mID;
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
    public int getDestinationStarID() {
        return mDestinationStarID;
    }
    public int getTargetFleetID() {
        return mTargetFleetID;
    }

    public void setStance(Stance stance) {
        mStance = stance;
    }

    /**
     * Split this fleet into two. The one we create will have size "otherSize" and we'll
     * have whatever is left over.
     */
    public Fleet split(float otherSize) {
        mNumShips -= otherSize;

        Fleet other = new Fleet();
        other.mID = 0;
        other.mKey = null;
        other.mStarID = mStarID;
        other.mStarKey = mStarKey;
        other.mSectorID = mSectorID;
        other.mDesignID = mDesignID;
        other.mEmpireID = mEmpireID;
        other.mEmpireKey = mEmpireKey;
        other.mNumShips = otherSize;
        other.mStance = mStance;
        other.mState = State.IDLE;
        other.mStateStartTime = DateTime.now();
        return other;
    }
}
