package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

public class Fleet extends BaseFleet {
    private int mID;
    private int mStarID;
    private int mSectorID;
    private Integer mEmpireID;
    private Integer mDestinationStarID;
    private Integer mTargetFleetID;

    public Fleet() {
    }
    public Fleet(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mKey = Integer.toString(mID);
        mStarID = res.getInt("star_id");
        mStarKey = Integer.toString(mStarID);
        mSectorID = res.getInt("sector_id");
        mDesignID = res.getString("design_id");
        mEmpireID = res.getInt("empire_id");
        if (mEmpireID != null) {
            mEmpireKey = Integer.toString(mEmpireID);
        }
        mAllianceID = res.getInt("alliance_id");
        mNumShips = res.getFloat("num_ships");
        mStance = Stance.fromNumber(res.getInt("stance"));
        mState = State.fromNumber(res.getInt("state"));
        mStateStartTime = res.getDateTime("state_start_time");
        mEta = res.getDateTime("eta");
        mDestinationStarID = res.getInt("target_star_id");
        if (mDestinationStarID != null) {
            mDestinationStarKey = Integer.toString(mDestinationStarID);
        }

        mTargetFleetID = res.getInt("target_fleet_id");
        if (mTargetFleetID != null) {
            mTargetFleetKey = Integer.toString(mTargetFleetID);
        }

        mNotes = res.getString("notes");
    }
    public Fleet(Empire empire, Star star, String designID, float numShips) {
        mStarID = star.getID();
        mStarKey = Integer.toString(mStarID);
        mSectorID = star.getSectorID();
        mDesignID = designID;
        if (empire != null) {
            mEmpireID = empire.getID();
            mEmpireKey = Integer.toString(mEmpireID);
            if (empire.getAlliance() != null) {
                mAllianceID = ((Alliance) empire.getAlliance()).getID();
            }
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
    public Integer getEmpireID() {
        return mEmpireID;
    }
    public Integer getDestinationStarID() {
        return mDestinationStarID;
    }
    public Integer getTargetFleetID() {
        return mTargetFleetID;
    }
    public void setEta(DateTime eta) {
        mEta = eta;
    }

    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(id);

        if (mUpgrades != null) {
            for (BaseFleetUpgrade baseUpgrade : mUpgrades) {
                FleetUpgrade upgrade = (FleetUpgrade) baseUpgrade;
                upgrade.setFleetID(id);
            }
        }
    }
    public void setStance(Stance stance) {
        mStance = stance;
    }
    public void setNotes(String notes) {
        mNotes = notes;
    }

    @Override
    public FleetUpgrade getUpgrade(String upgradeID) {
        return (FleetUpgrade) super.getUpgrade(upgradeID);
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
        mTargetFleetID = null;
    }

    @Override
    public void idle(DateTime now) {
        super.idle(now);
        mDestinationStarID = null;
        mTargetFleetID = null;
    }

    @Override
    public void attack(DateTime now) {
        super.attack(now);
        mDestinationStarID = null;
        mTargetFleetID = null;
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
        if (mUpgrades != null) {
            other.mUpgrades = new ArrayList<BaseFleetUpgrade>();
            for (BaseFleetUpgrade upgrade : mUpgrades) {
                other.mUpgrades.add(new FleetUpgrade((FleetUpgrade) upgrade));
            }
        }
        return other;
    }

    public void setNumShips(float numShips) {
        mNumShips = numShips;
    }

    @Override
    protected BaseFleetUpgrade createUpgrade(Messages.FleetUpgrade pb) {
        FleetUpgrade fleetUpgrade;

        String upgradeID = pb.getUpgradeId();
        if (upgradeID.equals("boost")) {
            fleetUpgrade = new FleetUpgrade.BoostFleetUpgrade();
        } else {
            fleetUpgrade = new FleetUpgrade();
        }

        if (pb != null) {
            fleetUpgrade.fromProtocolBuffer(this, pb);
        }
        return fleetUpgrade;
    }
}
