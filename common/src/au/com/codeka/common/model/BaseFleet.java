package au.com.codeka.common.model;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import au.com.codeka.common.protobuf.Messages;

public abstract class BaseFleet {
    protected String mKey;
    protected String mEmpireKey;
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

    public String getKey() {
        return mKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
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
        return mUpgrades;
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

    public float getTimeToDestination(BaseStar srcStar, BaseStar destStar) {
        if (mEta == null) {
            return 0.0f;
        }

        DateTime now = DateTime.now(DateTimeZone.UTC);
        return (Seconds.secondsBetween(now, mEta).getSeconds() / 3600.0f);
    }

    protected abstract BaseFleetUpgrade createUpgrade(Messages.FleetUpgrade pb);

    public void fromProtocolBuffer(Messages.Fleet pb) {
        mKey = pb.getKey();
        if (pb.hasEmpireKey()) {
            mEmpireKey = pb.getEmpireKey();
        }
        mDesignID = pb.getDesignName();
        mNumShips = pb.getNumShips();
        mState = State.fromNumber(pb.getState().getNumber());
        mStateStartTime = new DateTime(pb.getStateStartTime() * 1000, DateTimeZone.UTC);
        mStarKey = pb.getStarKey();
        mDestinationStarKey = pb.getDestinationStarKey();
        mTargetFleetKey = pb.getTargetFleetKey();
        mTargetColonyKey = pb.getTargetColonyKey();
        if (pb.hasStance()) {
            mStance = Stance.fromNumber(pb.getStance().getNumber());
        } else {
            mStance = Stance.NEUTRAL;
        }
        if (pb.hasEta()) {
            mEta = new DateTime(pb.getEta() * 1000, DateTimeZone.UTC);
        }
        if (pb.hasTimeDestroyed()) {
            mTimeDestroyed = new DateTime(pb.getTimeDestroyed() * 1000, DateTimeZone.UTC);
        }
        mUpgrades = new ArrayList<BaseFleetUpgrade>();
        for (Messages.FleetUpgrade upgrade_pb : pb.getUpgradesList()) {
            mUpgrades.add(createUpgrade(upgrade_pb));
        }
    }

    public void toProtocolBuffer(Messages.Fleet.Builder pb) {
        pb.setKey(mKey);
        if (mEmpireKey != null) {
            pb.setEmpireKey(mEmpireKey);
        }
        pb.setDesignName(mDesignID);
        pb.setNumShips(mNumShips);
        pb.setState(Messages.Fleet.FLEET_STATE.valueOf(mState.getValue()));
        pb.setStance(Messages.Fleet.FLEET_STANCE.valueOf(mStance.getValue()));
        pb.setStateStartTime(mStateStartTime.getMillis() / 1000);
        pb.setStarKey(mStarKey);
        if (mDestinationStarKey != null) {
            pb.setDestinationStarKey(mDestinationStarKey);
        }
        if (mTargetFleetKey != null) {
            pb.setTargetFleetKey(mTargetFleetKey);
        }
        if (mTargetColonyKey != null) {
            pb.setTargetColonyKey(mTargetColonyKey);
        }
        if (mEta != null) {
            pb.setEta(mEta.getMillis() / 1000);
        }
        if (mTimeDestroyed != null) {
            pb.setTimeDestroyed(mTimeDestroyed.getMillis() / 1000);
        }
        if (mUpgrades != null) {
            for (BaseFleetUpgrade baseFleetUpgrade : mUpgrades) {
                Messages.FleetUpgrade.Builder upgrade_pb = Messages.FleetUpgrade.newBuilder();
                baseFleetUpgrade.toProtocolBuffer(upgrade_pb);
                pb.addUpgrades(upgrade_pb);
            }
        }
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
