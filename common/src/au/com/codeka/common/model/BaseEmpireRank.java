package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

public class BaseEmpireRank {
    protected String mEmpireKey;
    protected int mTotalStars;
    protected int mTotalColonies;
    protected int mTotalBuildings;
    protected int mTotalShips;
    protected int mRank;
    protected int mLastRank;

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

    public void toProtocolBuffer(Messages.EmpireRank.Builder pb) {
        pb.setEmpireKey(mEmpireKey);
        pb.setTotalStars(mTotalStars);
        pb.setTotalColonies(mTotalColonies);
        pb.setTotalBuildings(mTotalBuildings);
        pb.setTotalShips(mTotalShips);
        pb.setRank(mRank);
        pb.setLastRank(mLastRank);
    }
}
