package au.com.codeka.common.model;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import au.com.codeka.common.protobuf.Fleet;
import au.com.codeka.common.protobuf.FleetUpgrade;

public abstract class BaseFleet {
    protected String mKey;
    protected String mEmpireKey;
    protected Integer mAllianceID;
    protected String mDesignID;
    protected float mNumShips;
    protected State mState;
    protected DateTime mStateStartTime;
    protected String mStarKey;
    protected String mDestinationStarKey;
    protected String mTargetFleetKey;
    protected String mTargetColonyKey;
    protected Stance mStance;
    protected DateTime mEta;
    protected DateTime mTimeDestroyed;
    protected ArrayList<BaseFleetUpgrade> mUpgrades;
    protected String mNotes;

    public String getKey() {
        return mKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public Integer getAllianceID() {
        return mAllianceID;
    }
    public String getDesignID() {
        return mDesignID;
    }
    public float getNumShips() {
        return mNumShips;
    }
    public void setNumShips(float numShips) {
        if (numShips < 0.75f) {
            mNumShips = 0.0f;
        } else {
            mNumShips = numShips;
        }
    }
    public State getState() {
        return mState;
    }
    public void setState(State state, DateTime now) {
        mState = state;
        mStateStartTime = now;
    }
    public DateTime getStateStartTime() {
        return mStateStartTime;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public String getDestinationStarKey() {
        return mDestinationStarKey;
    }
    public String getTargetFleetKey() {
        return mTargetFleetKey;
    }
    public void setTargetFleetKey(String fleetKey) {
        mTargetFleetKey = fleetKey;
    }
    public String getTargetColonyKey() {
        return mTargetColonyKey;
    }
    public Stance getStance() {
        return mStance;
    }
    public DateTime getEta() {
        return mEta;
    }
    public DateTime getTimeDestroyed() {
        return mTimeDestroyed;
    }
    public void setTimeDestroyed(DateTime time) {
        mTimeDestroyed = time;
    }
    public ArrayList<BaseFleetUpgrade> getUpgrades() {
        if (mUpgrades == null) {
            mUpgrades = new ArrayList<BaseFleetUpgrade>();
        }
        return mUpgrades;
    }
    public String getNotes() {
        return mNotes;
    }

    public BaseFleetUpgrade getUpgrade(String upgradeID) {
        if (mUpgrades == null) {
            return null;
        }
        for (BaseFleetUpgrade baseFleetUpgrade : mUpgrades) {
            if (baseFleetUpgrade.getUpgradeID().equals(upgradeID)) {
                return baseFleetUpgrade;
            }
        }
        return null;
    }
    public boolean hasUpgrade(String upgradeID) {
        return (getUpgrade(upgradeID) != null);
    }

    public void move(DateTime now, String destinationStarKey, DateTime eta) {
        mState = State.MOVING;
        mStateStartTime = now;
        mDestinationStarKey = destinationStarKey;
        mEta = eta;
        mTargetFleetKey = null;
    }

    /**
     * When the fleet stops attacking, moving, etc it goes back to being idle
     */
    public void idle(DateTime now) {
        mState = State.IDLE;
        mStateStartTime = now;
        mDestinationStarKey = null;
        mEta = null;
        mTargetFleetKey = null;
    }

    /**
     * Switch to attack mode
     */
    public void attack(DateTime now) {
        mState = State.ATTACKING;
        mStateStartTime = now;
        mDestinationStarKey = null;
        mEta = null;
        mTargetFleetKey = null;
    }

    public float getTimeToDestination() {
        if (mEta == null) {
            return 0.0f;
        }

        DateTime now = DateTime.now(DateTimeZone.UTC);
        return (Seconds.secondsBetween(now, mEta).getSeconds() / 3600.0f);
    }
    public float getTimeFromSource() {
        if (mStateStartTime == null || mState != State.MOVING) {
            return 0.0f;
        }

        DateTime now = DateTime.now(DateTimeZone.UTC);
        return (Seconds.secondsBetween(mStateStartTime, now).getSeconds() / 3600.0f);
    }

    protected abstract BaseFleetUpgrade createUpgrade(FleetUpgrade pb);

    public void fromProtocolBuffer(Fleet pb) {
        mKey = pb.key;
        mEmpireKey = pb.empire_key;
        mAllianceID = pb.alliance_id;
        mDesignID = pb.design_name;
        mNumShips = pb.num_ships;
        mState = State.fromNumber(pb.state.getValue());
        mStateStartTime = new DateTime(pb.state_start_time * 1000, DateTimeZone.UTC);
        mStarKey = pb.star_key;
        mDestinationStarKey = pb.destination_star_key;
        mTargetFleetKey = pb.target_fleet_key;
        mTargetColonyKey = pb.target_colony_key;
        if (pb.stance != null) {
            mStance = Stance.fromNumber(pb.stance.getValue());
        } else {
            mStance = Stance.NEUTRAL;
        }
        if (pb.eta != null) {
            mEta = new DateTime(pb.eta * 1000, DateTimeZone.UTC);
        }
        if (pb.time_destroyed != null) {
            mTimeDestroyed = new DateTime(pb.time_destroyed * 1000, DateTimeZone.UTC);
        }
        mUpgrades = new ArrayList<>();
        if (pb.upgrades != null) {
            for (FleetUpgrade upgrade_pb : pb.upgrades) {
                mUpgrades.add(createUpgrade(upgrade_pb));
            }
        }
        mNotes = pb.notes;
    }

    public void toProtocolBuffer(Fleet pb) {
        pb.key = mKey;
        pb.empire_key = mEmpireKey;
        pb.alliance_id = mAllianceID;
        pb.design_name = mDesignID;
        pb.num_ships = mNumShips;
        pb.state = Fleet.FLEET_STATE.valueOf(mState.toString());
        pb.stance = Fleet.FLEET_STANCE.valueOf(mStance.toString());
        pb.state_start_time = mStateStartTime.getMillis() / 1000;
        pb.star_key = mStarKey;
        pb.destination_star_key = mDestinationStarKey;
        pb.target_fleet_key = mTargetFleetKey;
        pb.target_colony_key = mTargetColonyKey;
        if (mEta != null) {
            pb.eta = mEta.getMillis() / 1000;
        }
        if (mTimeDestroyed != null) {
            pb.time_destroyed = mTimeDestroyed.getMillis() / 1000;
        }
        if (mUpgrades != null) {
            pb.upgrades = new ArrayList<>();
            for (BaseFleetUpgrade baseFleetUpgrade : mUpgrades) {
                FleetUpgrade upgrade_pb = new FleetUpgrade();
                baseFleetUpgrade.toProtocolBuffer(upgrade_pb);
                pb.upgrades.add(upgrade_pb);
            }
        }
        pb.notes = mNotes;
    }

    public enum State {
        IDLE(1),
        MOVING(2),
        ATTACKING(3);

        private int mValue;

        State(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static State fromNumber(int value) {
            for(State s : State.values()) {
                if (s.getValue() == value) {
                    return s;
                }
            }

            return State.IDLE;
        }
    }

    public enum Stance {
        PASSIVE(1),
        NEUTRAL(2),
        AGGRESSIVE(3);

        private int mValue;

        Stance(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static Stance fromNumber(int value) {
            for(Stance s : Stance.values()) {
                if (s.getValue() == value) {
                    return s;
                }
            }

            return Stance.NEUTRAL;
        }
    }
}
