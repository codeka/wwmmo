package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class BaseFleet {
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
    public State getState() {
        return mState;
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
    public String getTargetColonyKey() {
        return mTargetColonyKey;
    }
    public Stance getStance() {
        return mStance;
    }
    public DateTime getEta() {
        return mEta;
    }

    public float getTimeToDestination(BaseStar srcStar, BaseStar destStar) {/*
        ShipDesign design = ShipDesignManager.getInstance().getDesign(mDesignID);
        float distanceInParsecs = SectorManager.getInstance().distanceInParsecs(srcStar, destStar);
        float totalTimeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();

        DateTime now = DateTime.now(DateTimeZone.UTC);
        float timeSoFarInHours = (Seconds.secondsBetween(mStateStartTime, now).getSeconds() / 3600.0f);

        return totalTimeInHours - timeSoFarInHours;*/
        return 0;
    }

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
