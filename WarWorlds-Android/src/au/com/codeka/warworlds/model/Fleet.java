package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;

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
            return f;
        }

        @Override
        public Fleet[] newArray(int size) {
            return new Fleet[size];
        }
    };

    public static Fleet fromProtocolBuffer(warworlds.Warworlds.Fleet pb) {
        Fleet f = new Fleet();
        f.mKey = pb.getKey();
        f.mEmpireKey = pb.getEmpireKey();
        f.mDesignID = pb.getDesignName();
        f.mNumShips = pb.getNumShips();
        f.mState = State.fromNumber(pb.getState().getNumber());
        f.mStateStartTime = new DateTime(pb.getStateStartTime() * 1000, DateTimeZone.UTC);
        f.mStarKey = pb.getStarKey();
        f.mDestinationStarKey = pb.getDestinationStarKey();
        f.mTargetFleetKey = pb.getTargetFleetKey();
        f.mTargetColonyKey = pb.getTargetColonyKey();
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
}
