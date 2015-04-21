package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.EmpireRank;

public class BaseEmpireRank {
    protected String mEmpireKey;
    protected int mTotalStars;
    protected int mTotalColonies;
    protected int mTotalBuildings;
    protected int mTotalShips;
    protected int mTotalPopulation;
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
    public int getTotalPopulation() {
        return mTotalPopulation;
    }
    public int getRank() {
        return mRank;
    }
    public int getLastRank() {
        return mLastRank;
    }

    public void fromProtocolBuffer(EmpireRank pb) {
        mEmpireKey = pb.empire_key;
        mTotalStars = pb.total_stars;
        mTotalColonies = pb.total_colonies;
        mTotalBuildings = pb.total_buildings;
        mTotalShips = pb.total_ships;
        mTotalPopulation = pb.total_population;
        mRank = pb.rank;
        mLastRank = pb.last_rank;
    }

    public void toProtocolBuffer(EmpireRank.Builder pb) {
        pb.empire_key = mEmpireKey;
        pb.total_stars = mTotalStars;
        pb.total_colonies = mTotalColonies;
        pb.total_buildings = mTotalBuildings;
        pb.total_ships = mTotalShips;
        pb.total_population = mTotalPopulation;
        pb.rank = mRank;
        pb.last_rank = mLastRank;
    }
}
