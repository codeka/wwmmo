package au.com.codeka.warworlds.model;

import au.com.codeka.warworlds.model.protobuf.Messages;

public class EmpireRank {
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

    public static EmpireRank fromProtocolBuffer(Messages.EmpireRank pb) {
        EmpireRank empireRank = new EmpireRank();
        empireRank.mEmpireKey = pb.getEmpireKey();
        empireRank.mTotalStars = pb.getTotalStars();
        empireRank.mTotalColonies = pb.getTotalColonies();
        empireRank.mTotalBuildings = pb.getTotalBuildings();
        empireRank.mTotalShips = pb.getTotalShips();
        empireRank.mRank = pb.getRank();
        empireRank.mLastRank = pb.getLastRank();
        return empireRank;
    }
}
