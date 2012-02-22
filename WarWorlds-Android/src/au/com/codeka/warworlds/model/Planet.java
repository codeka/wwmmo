package au.com.codeka.warworlds.model;

import au.com.codeka.warworlds.R;


public class Planet {
    private static PlanetType[] sPlanetTypes = {
        new PlanetType.Builder().setDisplayName("Gas Giant")
                                .setIconId(R.drawable.planet_icon_gasgiant)
                                .build(),
        new PlanetType.Builder().setDisplayName("Radiated")
                                .setIconId(R.drawable.planet_icon_radiated)
                                .build(),
        new PlanetType.Builder().setDisplayName("Inferno")
                                .setIconId(R.drawable.planet_icon_inferno)
                                .build(),
        new PlanetType.Builder().setDisplayName("Asteroids")
                                .setIconId(R.drawable.planet_icon_asteroids)
                                .build(),
        new PlanetType.Builder().setDisplayName("Water")
                                .setIconId(R.drawable.planet_icon_water)
                                .build(),
        new PlanetType.Builder().setDisplayName("Toxic")
                                .setIconId(R.drawable.planet_icon_toxic)
                                .build(),
        new PlanetType.Builder().setDisplayName("Desert")
                                .setIconId(R.drawable.planet_icon_desert)
                                .build(),
        new PlanetType.Builder().setDisplayName("Swamp")
                                .setIconId(R.drawable.planet_icon_swamp)
                                .build(),
        new PlanetType.Builder().setDisplayName("Terran")
                                .setIconId(R.drawable.planet_icon_terran)
                                .build()
    };

    private Star mStar;
    private int mIndex;
    private PlanetType mPlanetType;
    private int mSize;

    public Star getStar() {
        return mStar;
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

    /**
     * Converts the given Planet protocol buffer into a \c Planet.
     */
    public static Planet fromProtocolBuffer(Star star, warworlds.Warworlds.Planet pb) {
        Planet p = new Planet();
        p.mStar = star;
        p.mIndex = pb.getIndex();
        p.mPlanetType = sPlanetTypes[pb.getPlanetType().getNumber() - 1];
        p.mSize = pb.getSize();

        return p;
    }

    public static class PlanetType {
        private String mDisplayName;
        private int mIconID;

        public String getDisplayName() {
            return mDisplayName;
        }
        public int getIconID() {
            return mIconID;
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

            public Builder setIconId(int id) {
                mPlanetType.mIconID = id;
                return this;
            }

            public PlanetType build() {
                return mPlanetType;
            }
        }
    }
}
