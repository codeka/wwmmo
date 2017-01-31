package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;
import java.math.BigInteger;

public class BaseEmpireRank {
    protected String mEmpireKey;
    protected BigInteger mTotalStars = BigInteger.valueOf(0);
    protected BigInteger mTotalColonies = BigInteger.valueOf(0);
    protected BigInteger mTotalBuildings = BigInteger.valueOf(0);
    protected BigInteger mTotalShips = BigInteger.valueOf(0);
    protected BigInteger mTotalPopulation = BigInteger.valueOf(0);
    protected int mRank;
    protected int mLastRank;

    public String getEmpireKey() {
        return mEmpireKey;
    }
    public BigInteger getTotalStars() {
        return mTotalStars;
    }
    public BigInteger getTotalColonies() {
        return mTotalColonies;
    }
    public BigInteger getTotalBuildings() {
        return mTotalBuildings;
    }
    public BigInteger getTotalShips() {
        return mTotalShips;
    }
    public BigInteger getTotalPopulation() {
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
