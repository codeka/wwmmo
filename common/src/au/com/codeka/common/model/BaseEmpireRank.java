package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;
import java.math.BigInteger;

public class BaseEmpireRank {
    protected String mEmpireKey;
    protected long mTotalStars;
    protected long mTotalColonies;
    protected long mTotalBuildings;
    protected long mTotalShips;
    protected long mTotalPopulation;
    protected int mRank;
    protected int mLastRank;

    public String getEmpireKey() {
        return mEmpireKey;
    }
    public long getTotalStars() {
        return mTotalStars;
    }
    public long getTotalColonies() {
        return mTotalColonies;
    }
    public long getTotalBuildings() {
        return mTotalBuildings;
    }
    public long getTotalShips() {
        return mTotalShips;
    }
    public long getTotalPopulation() {
        return mTotalPopulation;
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
        mTotalPopulation = pb.getTotalPopulation();
        mRank = pb.getRank();
        mLastRank = pb.getLastRank();
    }

    public void toProtocolBuffer(Messages.EmpireRank.Builder pb) {
        pb.setEmpireKey(mEmpireKey);
        pb.setTotalStars(mTotalStars);
        pb.setTotalColonies(mTotalColonies);
        pb.setTotalBuildings(mTotalBuildings);
        pb.setTotalShips(mTotalShips);
        pb.setTotalPopulation(mTotalPopulation);
        pb.setRank(mRank);
        pb.setLastRank(mLastRank);
    }
}
