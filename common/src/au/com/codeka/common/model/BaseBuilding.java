package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

/**
 * Represents a single building on a colony.
 */
public class BaseBuilding {
    protected String mKey;
    protected String mColonyKey;
    protected String mEmpireKey;
    protected String mDesignID;
    protected int mLevel;

    public String getKey() {
        return mKey;
    }
    public String getColonyKey() {
        return mColonyKey;
    }
    public String getDesignID() {
        return mDesignID;
    }
    public int getLevel() {
        return mLevel;
    }

    public void fromProtocolBuffer(Messages.Building pb) {
        mKey = pb.getKey();
        mColonyKey = pb.getColonyKey();
        mDesignID = pb.getDesignName();
        mLevel = pb.getLevel();
    }

    public void toProtocolBuffer(Messages.Building.Builder pb) {
        pb.setKey(mKey);
        pb.setColonyKey(mColonyKey);
        pb.setDesignName(mDesignID);
        pb.setLevel(mLevel);
    }
}
