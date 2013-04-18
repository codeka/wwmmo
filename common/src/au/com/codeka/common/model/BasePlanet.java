package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;


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
    public void fromProtocolBuffer(BaseStar star, Messages.Planet pb) {
        mStar = star;
        mIndex = pb.getIndex();
        mPlanetType = sPlanetTypes[pb.getPlanetType().getNumber() - 1];
        mSize = pb.getSize();
        if (pb.hasPopulationCongeniality()) {
            mPopulationCongeniality = pb.getPopulationCongeniality();
        }
        if (pb.hasFarmingCongeniality()) {
            mFarmingCongeniality = pb.getFarmingCongeniality();
        }
        if (pb.hasMiningCongeniality()) {
            mMiningCongeniality = pb.getMiningCongeniality();
        }
    }

    public void toProtocolBuffer(Messages.Planet.Builder pb) {
        pb.setIndex(mIndex);
        pb.setPlanetType(Messages.Planet.PLANET_TYPE.valueOf(mPlanetType.mIndex + 1));
        pb.setSize(mSize);
        pb.setPopulationCongeniality(mPopulationCongeniality);
        pb.setFarmingCongeniality(mFarmingCongeniality);
        pb.setMiningCongeniality(mMiningCongeniality);
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
