package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.Star;


/**
 * The @see StarType describes details about a star based on whether it's blue, white, neutron, etc.
 */
public class StarType {
    private static StarType[] sStarTypes = {
        new StarType.Builder().setIndex(0)
                              .setDisplayName("Blue")
                              .setInternalName("blue")
                              .setShortName("B")
                              .build(),
        new StarType.Builder().setIndex(1)
                              .setDisplayName("White")
                              .setInternalName("white")
                              .setShortName("W")
                              .build(),
        new StarType.Builder().setIndex(2)
                              .setDisplayName("Yellow")
                              .setInternalName("yellow")
                              .setShortName("Y")
                              .build(),
        new StarType.Builder().setIndex(3)
                              .setDisplayName("Orange")
                              .setInternalName("orange")
                              .setShortName("O")
                              .build(),
        new StarType.Builder().setIndex(4)
                              .setDisplayName("Red")
                              .setInternalName("red")
                              .setShortName("R")
                              .build(),
        new StarType.Builder().setIndex(5)
                              .setDisplayName("Neutron")
                              .setInternalName("neutron")
                              .setShortName("N")
                              .setBaseSize(1.0)
                              .setImageScale(4.0)
                              .build(),
        new StarType.Builder().setIndex(6)
                              .setDisplayName("Black Hole")
                              .setInternalName("black-hole")
                              .setShortName("BH")
                              .build()
    };

    public static StarType get(Star star) {
        return sStarTypes[star.classification.ordinal()];
    }

    private int mIndex;
    private String mDisplayName;
    private String mInternalName;
    private String mShortName;
    private double mBaseSize;
    private double mImageScale;

    public int getIndex() {
        return mIndex;
    }
    public String getDisplayName() {
        return mDisplayName;
    }
    public String getInternalName() {
        return mInternalName;
    }
    public String getShortName() {
        return mShortName;
    }
    public String getBitmapBasePath() {
        return "stars/"+mInternalName;
    }

    /**
     * Gets the 'base size' of the star, which controls the "planet size" setting when
     * we render the image.
     */
    public double getBaseSize() {
        return mBaseSize;
    }

    /**
     * When generating the image, we scale the final bitmap by this amount. Default is 1.0
     * obviously.
     */
    public double getImageScale() {
        return mImageScale;
    }

    public static class Builder {
        private StarType mStarType;

        public Builder() {
            mStarType = new StarType();
            mStarType.mBaseSize = 8.0;
            mStarType.mImageScale = 1.0;
        }

        public Builder setIndex(int index) {
            mStarType.mIndex = index;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            mStarType.mDisplayName = displayName;
            return this;
        }

        public Builder setInternalName(String internalName) {
            mStarType.mInternalName = internalName;
            return this;
        }

        public Builder setShortName(String shortName) {
            mStarType.mShortName = shortName;
            return this;
        }

        public Builder setBaseSize(double baseSize) {
            mStarType.mBaseSize = baseSize;
            return this;
        }

        public Builder setImageScale(double scale) {
            mStarType.mImageScale = scale;
            return this;
        }

        public StarType build() {
            return mStarType;
        }
    }
}