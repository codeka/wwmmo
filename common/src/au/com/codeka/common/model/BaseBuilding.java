package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

/**
 * Represents a single building on a colony.
 */
public class BaseBuilding {
    protected String mKey;
    protected String mColonyKey;
    protected String mDesignName;
    protected int mLevel;

    public String getKey() {
        return mKey;
    }
    public String getColonyKey() {
        return mColonyKey;
    }
    public String getDesignName() {
        return mDesignName;
    }
    public int getLevel() {
        return mLevel;
    }

    public void fromProtocolBuffer(Messages.Building pb) {
        mKey = pb.getKey();
        mColonyKey = pb.getColonyKey();
        mDesignName = pb.getDesignName();
        mLevel = pb.getLevel();
    }
}
