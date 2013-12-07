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

    public void fromProtocolBuffer(Messages.Building pb) {
        mKey = pb.getKey();
        mColonyKey = pb.getColonyKey();
        mDesignID = pb.getDesignName();
        mLevel = pb.getLevel();
        if (pb.hasNotes()) {
            mNotes = pb.getNotes();
        }
    }

    public void toProtocolBuffer(Messages.Building.Builder pb) {
        pb.setKey(mKey);
        pb.setColonyKey(mColonyKey);
        pb.setDesignName(mDesignID);
        pb.setLevel(mLevel);
        if (mNotes != null) {
            pb.setNotes(mNotes);
        }
    }
}
