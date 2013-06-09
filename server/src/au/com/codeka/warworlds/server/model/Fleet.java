package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;

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
        mDesignID = rs.getString("design_id");
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
    public Fleet(Empire empire, Star star, String designID, float numShips) {
        mStarID = star.getID();
        mStarKey = Integer.toString(mStarID);
        mSectorID = star.getSectorID();
        mDesignID = designID;
        if (empire != null) {
            mEmpireID = empire.getID();
            mEmpireKey = Integer.toString(mEmpireID);
        }
        mNumShips = numShips;
        mStance = Stance.AGGRESSIVE;
        mState = State.IDLE;
        mStateStartTime = DateTime.now();
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

    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(id);
    }
    public void setStance(Stance stance) {
        mStance = stance;
    }
    public void setTargetFleetID(int id) {
        mTargetFleetID = id;
        mTargetFleetKey = Integer.toString(id);
    }

    public ShipDesign getDesign() {
        return (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, mDesignID);
    }

    /**
     * Returns a float, between 0 and 1, that indicates how close we are to our destination.
     * This method assumes we're actually moving...
     */
    public float getMovementProgress() {
        if (mEta == null) {
            return 0.0f;
        }

        float totalMovementTimeInHours = Seconds.secondsBetween(mStateStartTime, mEta).getSeconds() / 3600.0f;
        if (totalMovementTimeInHours < 0.0001) {
            return 0.0f;
        }

        float currentMovementTimeInHours = Seconds.secondsBetween(mStateStartTime, DateTime.now()).getSeconds() / 3600.0f;
        if (currentMovementTimeInHours > totalMovementTimeInHours) {
            return 1.0f;
        }

        return currentMovementTimeInHours / totalMovementTimeInHours;
    }

    @Override
    public void move(DateTime now, String destinationStarKey, DateTime eta) {
        super.move(now, destinationStarKey, eta);
        mDestinationStarID = Integer.parseInt(destinationStarKey);
        mTargetFleetID = 0;
    }

    @Override
    public void idle(DateTime now) {
        super.idle(now);
        mDestinationStarID = 0;
        mTargetFleetID = 0;
    }

    @Override
    public void attack(DateTime now) {
        super.attack(now);
        mDestinationStarID = 0;
        mTargetFleetID = 0;
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

    public void setNumShips(float numShips) {
        mNumShips = numShips;
    }
}
