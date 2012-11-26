package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class Fleet implements Parcelable {
    private String mKey;
    private String mEmpireKey;
    private String mDesignID;
    private int mNumShips;
    private State mState;
    private DateTime mStateStartTime;
    private String mStarKey;
    private String mDestinationStarKey;
    private String mTargetFleetKey;
    private String mTargetColonyKey;
    private Stance mStance;

    public String getKey() {
        return mKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public String getDesignID() {
        return mDesignID;
    }
    public int getNumShips() {
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

    public float getTimeToDestination(StarSummary srcStar, StarSummary destStar) {
        ShipDesign design = ShipDesignManager.getInstance().getDesign(mDesignID);
        float distanceInParsecs = SectorManager.getInstance().distanceInParsecs(srcStar, destStar);
        float totalTimeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();

        DateTime now = DateTime.now(DateTimeZone.UTC);
        float timeSoFarInHours = (Seconds.secondsBetween(mStateStartTime, now).getSeconds() / 3600.0f);

        return totalTimeInHours - timeSoFarInHours;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mEmpireKey);
        parcel.writeString(mDesignID);
        parcel.writeInt(mNumShips);
        parcel.writeInt(mState.getValue());
        parcel.writeLong(mStateStartTime.getMillis());
        parcel.writeString(mStarKey);
        parcel.writeString(mDestinationStarKey);
        parcel.writeString(mTargetFleetKey);
        parcel.writeString(mTargetColonyKey);
        parcel.writeInt(mStance.getValue());
    }

    public static final Parcelable.Creator<Fleet> CREATOR
                = new Parcelable.Creator<Fleet>() {
        @Override
        public Fleet createFromParcel(Parcel parcel) {
            Fleet f = new Fleet();
            f.mKey = parcel.readString();
            f.mEmpireKey = parcel.readString();
            f.mDesignID = parcel.readString();
            f.mNumShips = parcel.readInt();
            f.mState = State.fromNumber(parcel.readInt());
            f.mStateStartTime = new DateTime(parcel.readLong(), DateTimeZone.UTC);
            f.mStarKey = parcel.readString();
            f.mDestinationStarKey = parcel.readString();
            f.mTargetFleetKey = parcel.readString();
            f.mTargetColonyKey = parcel.readString();
            f.mStance = Stance.fromNumber(parcel.readInt());
            return f;
        }

        @Override
        public Fleet[] newArray(int size) {
            return new Fleet[size];
        }
    };

    public static Fleet fromProtocolBuffer(Messages.Fleet pb) {
        Fleet f = new Fleet();
        f.mKey = pb.getKey();
        if (pb.hasEmpireKey()) {
            f.mEmpireKey = pb.getEmpireKey();
        }
        f.mDesignID = pb.getDesignName();
        f.mNumShips = (int) Math.ceil(pb.getNumShips());
        f.mState = State.fromNumber(pb.getState().getNumber());
        f.mStateStartTime = new DateTime(pb.getStateStartTime() * 1000, DateTimeZone.UTC);
        f.mStarKey = pb.getStarKey();
        f.mDestinationStarKey = pb.getDestinationStarKey();
        f.mTargetFleetKey = pb.getTargetFleetKey();
        f.mTargetColonyKey = pb.getTargetColonyKey();
        if (pb.hasStance()) {
            f.mStance = Stance.fromNumber(pb.getStance().getNumber());
        } else {
            f.mStance = Stance.NEUTRAL;
        }
        return f;
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
