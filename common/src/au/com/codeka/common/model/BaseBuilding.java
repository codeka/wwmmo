package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Building;

/**
 * Represents a single building on a colony.
 */
public class BaseBuilding {
    protected String mKey;
    protected String mColonyKey;
    protected String mEmpireKey;
    protected String mDesignID;
    protected int mLevel;
    protected String mNotes;

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
    public String getNotes() {
        return mNotes;
    }

    public void fromProtocolBuffer(Building pb) {
        mKey = pb.key;
        mColonyKey = pb.colony_key;
        mDesignID = pb.design_name;
        mLevel = pb.level;
        mNotes = pb.notes;
    }

    public void toProtocolBuffer(Building.Builder pb) {
        pb.key = mKey;
        pb.colony_key = mColonyKey;
        pb.design_name = mDesignID;
        pb.level = mLevel;
        pb.notes = mNotes;
    }
}
