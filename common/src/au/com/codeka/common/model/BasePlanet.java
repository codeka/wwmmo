package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Planet;


public class BasePlanet {
    protected static PlanetType[] sPlanetTypes = {
        new PlanetType.Builder().setIndex(0)
                                .setDisplayName("Gas Giant")
                                .setInternalName("gasgiant")
                                .build(),
        new PlanetType.Builder().setIndex(1)
                                .setDisplayName("Radiated")
                                .setInternalName("radiated")
                                .build(),
        new PlanetType.Builder().setIndex(2)
                                .setDisplayName("Inferno")
                                .setInternalName("inferno")
                                .build(),
        new PlanetType.Builder().setIndex(3)
                                .setDisplayName("Asteroids")
                                .setInternalName("asteroids")
                                .build(),
        new PlanetType.Builder().setIndex(4)
                                .setDisplayName("Water")
                                .setInternalName("water")
                                .build(),
        new PlanetType.Builder().setIndex(5)
                                .setDisplayName("Toxic")
                                .setInternalName("toxic")
                                .build(),
        new PlanetType.Builder().setIndex(6)
                                .setDisplayName("Desert")
                                .setInternalName("desert")
                                .build(),
        new PlanetType.Builder().setIndex(7)
                                .setDisplayName("Swamp")
                                .setInternalName("swamp")
                                .build(),
        new PlanetType.Builder().setIndex(8)
                                .setDisplayName("Terran")
                                .setInternalName("terran")
                                .build()
    };

    protected BaseStar mStar;
    protected int mIndex;
    protected PlanetType mPlanetType;
    protected int mSize;
    protected int mPopulationCongeniality;
    protected int mFarmingCongeniality;
    protected int mMiningCongeniality;

    public BaseStar getStar() {
        return mStar;
    }
    public void setStar(BaseStar star) {
        mStar = star;
    }
    public int getIndex() {
        return mIndex;
    }
    public PlanetType getPlanetType() {
        return mPlanetType;
    }
    public int getSize() {
        return mSize;
    }
    public int getPopulationCongeniality() {
        return mPopulationCongeniality;
    }
    public int getFarmingCongeniality() {
        return mFarmingCongeniality;
    }
    public int getMiningCongeniality() {
        return mMiningCongeniality;
    }

    @Override
    public int hashCode() {
        return mStar.getKey().hashCode() ^ (mIndex * 632548);
    }

    /**
     * Converts the given Planet protocol buffer into a \c Planet.
     */
    public void fromProtocolBuffer(BaseStar star, Planet pb) {
        mStar = star;
        mIndex = pb.index;
        mPlanetType = sPlanetTypes[pb.planet_type.getValue() - 1];
        mSize = pb.size;
        if (pb.population_congeniality != null) {
            mPopulationCongeniality = pb.population_congeniality;
        }
        if (pb.farming_congeniality != null) {
            mFarmingCongeniality = pb.farming_congeniality;
        }
        if (pb.mining_congeniality != null) {
            mMiningCongeniality = pb.mining_congeniality;
        }
    }

    public void toProtocolBuffer(Planet.Builder pb) {
        pb.index = mIndex;
        pb.planet_type = Planet.PLANET_TYPE.values()[mPlanetType.mIndex + 1];
        pb.size = mSize;
        pb.population_congeniality = mPopulationCongeniality;
        pb.farming_congeniality = mFarmingCongeniality;
        pb.mining_congeniality = mMiningCongeniality;
    }

    /**
     * Contains a definition of the planet "type". This should be kept largely in sync with
     * the planet types defined in model/sector.py in the server.
     */
    public static class PlanetType {
        private int mIndex;
        private String mDisplayName;
        private String mInternalName;
        public int getIndex() {
            return mIndex;
        }
        public String getDisplayName() {
            return mDisplayName;
        }
        public String getInternalName() {
            return mInternalName;
        }
        public String getBitmapBasePath() {
            return "planets/"+mInternalName;
        }

        public static class Builder {
            private PlanetType mPlanetType;

            public Builder() {
                mPlanetType = new PlanetType();
            }

            public Builder setIndex(int index) {
                mPlanetType.mIndex = index;
                return this;
            }

            public Builder setDisplayName(String displayName) {
                mPlanetType.mDisplayName = displayName;
                return this;
            }

            public Builder setInternalName(String name) {
                mPlanetType.mInternalName = name;
                return this;
            }

            public PlanetType build() {
                return mPlanetType;
            }
        }
    }
}
