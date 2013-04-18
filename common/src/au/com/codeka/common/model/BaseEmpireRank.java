package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

public class BaseEmpireRank {
    private String mEmpireKey;
    private int mTotalStars;
    private int mTotalColonies;
    private int mTotalBuildings;
    private int mTotalShips;
    private int mRank;
    private int mLastRank;

    public String getEmpireKey() {
        return mEmpireKey;
    }
    public int getTotalStars() {
        return mTotalStars;
    }
    public int getTotalColonies() {
        return mTotalColonies;
    }
    public int getTotalBuildings() {
        return mTotalBuildings;
    }
    public int getTotalShips() {
        return mTotalShips;
    }
    public int getRank() {
        return mRank;
    }
    public int getLastRank() {
        return mLastRank;
    }

    public void fromProtocolBuffer(Messages.EmpireRank pb) {
        mEmpireKey = pb.getEmpireKey();
        mTotalStars = pb.getTotalStars();
        mTotalColonies = pb.getTotalColonies();
        mTotalBuildings = pb.getTotalBuildings();
        mTotalShips = pb.getTotalShips();
        mRank = pb.getRank();
        mLastRank = pb.getLastRank();
    }
}
