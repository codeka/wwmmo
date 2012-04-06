package au.com.codeka.warworlds.model;

import au.com.codeka.warworlds.R;


public class Planet {
    private static PlanetType[] sPlanetTypes = {
        new PlanetType.Builder().setDisplayName("Gas Giant")
                                .setIconID(R.drawable.planet_icon_gasgiant)
                                .setMedID(R.drawable.planet_med_gasgiant)
                                .build(),
        new PlanetType.Builder().setDisplayName("Radiated")
                                .setIconID(R.drawable.planet_icon_radiated)
                                .setMedID(R.drawable.planet_med_radiated)
                                .build(),
        new PlanetType.Builder().setDisplayName("Inferno")
                                .setIconID(R.drawable.planet_icon_inferno)
                                .setMedID(R.drawable.planet_med_inferno)
                                .build(),
        new PlanetType.Builder().setDisplayName("Asteroids")
                                .setIconID(R.drawable.planet_icon_asteroids)
                                .setMedID(R.drawable.planet_med_asteroids)
                                .build(),
        new PlanetType.Builder().setDisplayName("Water")
                                .setIconID(R.drawable.planet_icon_water)
                                .setMedID(R.drawable.planet_med_water)
                                .build(),
        new PlanetType.Builder().setDisplayName("Toxic")
                                .setIconID(R.drawable.planet_icon_toxic)
                                .setMedID(R.drawable.planet_med_toxic)
                                .build(),
        new PlanetType.Builder().setDisplayName("Desert")
                                .setIconID(R.drawable.planet_icon_desert)
                                .setMedID(R.drawable.planet_med_desert)
                                .build(),
        new PlanetType.Builder().setDisplayName("Swamp")
                                .setIconID(R.drawable.planet_icon_swamp)
                                .setMedID(R.drawable.planet_med_swamp)
                                .build(),
        new PlanetType.Builder().setDisplayName("Terran")
                                .setIconID(R.drawable.planet_icon_terran)
                                .setMedID(R.drawable.planet_med_terran)
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
        private int mIconID;
        private int mMedID;

        public String getDisplayName() {
            return mDisplayName;
        }
        public int getIconID() {
            return mIconID;
        }
        public int getMedID() {
            return mMedID;
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

            public Builder setIconID(int id) {
                mPlanetType.mIconID = id;
                return this;
            }

            public Builder setMedID(int id) {
                mPlanetType.mMedID = id;
                return this;
            }

            public PlanetType build() {
                return mPlanetType;
            }
        }
    }
}
