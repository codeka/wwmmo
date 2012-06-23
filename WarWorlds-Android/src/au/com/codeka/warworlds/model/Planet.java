package au.com.codeka.warworlds.model;


public class Planet {
    private static PlanetType[] sPlanetTypes = {
        new PlanetType.Builder().setDisplayName("Gas Giant")
                                .setBitmapBasePath("planets/gasgiant")
                                .build(),
        new PlanetType.Builder().setDisplayName("Radiated")
                                .setBitmapBasePath("planets/radiated")
                                .build(),
        new PlanetType.Builder().setDisplayName("Inferno")
                                .setBitmapBasePath("planets/inferno")
                                .build(),
        new PlanetType.Builder().setDisplayName("Asteroids")
                                .setBitmapBasePath("planets/asteroids")
                                .build(),
        new PlanetType.Builder().setDisplayName("Water")
                                .setBitmapBasePath("planets/water")
                                .build(),
        new PlanetType.Builder().setDisplayName("Toxic")
                                .setBitmapBasePath("planets/toxic")
                                .build(),
        new PlanetType.Builder().setDisplayName("Desert")
                                .setBitmapBasePath("planets/desert")
                                .build(),
        new PlanetType.Builder().setDisplayName("Swamp")
                                .setBitmapBasePath("planets/swamp")
                                .build(),
        new PlanetType.Builder().setDisplayName("Terran")
                                .setBitmapBasePath("planets/terran")
                                .build()
    };

    private Star mStar;
    private String mKey;
    private int mIndex;
    private PlanetType mPlanetType;
    private int mSize;
    private int mPopulationCongeniality;
    private int mFarmingCongeniality;
    private int mMiningCongeniality;

    public Star getStar() {
        return mStar;
    }
    public String getKey() {
        return mKey;
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

    /**
     * Converts the given Planet protocol buffer into a \c Planet.
     */
    public static Planet fromProtocolBuffer(Star star, warworlds.Warworlds.Planet pb) {
        Planet p = new Planet();
        p.mStar = star;
        p.mKey = pb.getKey();
        p.mIndex = pb.getIndex();
        p.mPlanetType = sPlanetTypes[pb.getPlanetType().getNumber() - 1];
        p.mSize = pb.getSize();
        if (pb.hasPopulationCongeniality()) {
            p.mPopulationCongeniality = pb.getPopulationCongeniality();
        }
        if (pb.hasFarmingCongeniality()) {
            p.mFarmingCongeniality = pb.getFarmingCongeniality();
        }
        if (pb.hasMiningCongeniality()) {
            p.mMiningCongeniality = pb.getMiningCongeniality();
        }

        return p;
    }

    public static class PlanetType {
        private String mDisplayName;
        private String mBitmapBasePath;

        public String getDisplayName() {
            return mDisplayName;
        }
        public String getBitmapBasePath() {
            return mBitmapBasePath;
        }

        public static class Builder {
            private PlanetType mPlanetType;

            public Builder() {
                mPlanetType = new PlanetType();
            }

            public Builder setDisplayName(String displayName) {
                mPlanetType.mDisplayName = displayName;
                return this;
            }

            public Builder setBitmapBasePath(String path) {
                mPlanetType.mBitmapBasePath = path;
                return this;
            }

            public PlanetType build() {
                return mPlanetType;
            }
        }
    }
}
