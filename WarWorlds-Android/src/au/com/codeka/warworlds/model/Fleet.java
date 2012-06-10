package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class Fleet {
    private String mKey;
    private String mEmpireKey;
    private String mDesignName;
    private int mNumShips;
    // TODO: fleet state
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
    public String getDesignName() {
        return mDesignName;
    }
    public int getNumShips() {
        return mNumShips;
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

    public static Fleet fromProtocolBuffer(warworlds.Warworlds.Fleet pb) {
        Fleet f = new Fleet();
        f.mKey = pb.getKey();
        f.mEmpireKey = pb.getEmpireKey();
        f.mDesignName = pb.getDesignName();
        f.mNumShips = pb.getNumShips();
        // f.state = pb.getState()
        f.mStateStartTime = new DateTime(pb.getStateStartTime() * 1000, DateTimeZone.UTC);
        f.mStarKey = pb.getStarKey();
        f.mDestinationStarKey = pb.getDestinationStarKey();
        f.mTargetFleetKey = pb.getTargetFleetKey();
        f.mTargetColonyKey = pb.getTargetColonyKey();
        return f;
    }
}
