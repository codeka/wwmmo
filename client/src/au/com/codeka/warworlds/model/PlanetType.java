package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.Planet;

/**
 * Contains a definition of the planet "type". This should be kept largely in sync with
 * the planet types defined in model/sector.py in the server.
 */
public class PlanetType {
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

    public static PlanetType get(Planet.PLANET_TYPE type) {
        return sPlanetTypes[type.ordinal()];
    }
    public static PlanetType get(Planet planet) {
        return sPlanetTypes[planet.planet_type.ordinal()];
    }

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
