package au.com.codeka.warworlds.model;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;

/**
 * A \c StarSummary is a snapshot of information about a star that we can cache for a much
 * longer period of time (i.e. we cache it in the filesystem basically until the full star
 * is fetched). This is so we can do quicker look-ups of things like star names/icons without
 * having to do a full round-trip.
 */
public class StarSummary implements Parcelable {
    protected static StarType[] sStarTypes = {
        new StarType.Builder().setIndex(0)
                              .setDisplayName("Blue")
                              .setInternalName("blue")
                              .build(),
        new StarType.Builder().setIndex(1)
                              .setDisplayName("White")
                              .setInternalName("white")
                              .build(),
        new StarType.Builder().setIndex(2)
                              .setDisplayName("Yellow")
                              .setInternalName("yellow")
                              .build(),
        new StarType.Builder().setIndex(3)
                              .setDisplayName("Orange")
                              .setInternalName("orange")
                              .build(),
        new StarType.Builder().setIndex(4)
                              .setDisplayName("Red")
                              .setInternalName("red")
                              .build(),
        new StarType.Builder().setIndex(5)
                              .setDisplayName("Neutron")
                              .setInternalName("neutron")
                              .setBaseSize(1.0)
                              .setImageScale(4.0)
                              .build(),
        new StarType.Builder().setIndex(6)
                              .setDisplayName("Back Hole")
                              .setInternalName("black-hole")
                              .build()
    };

    private String mKey;
    private String mName;
    private StarType mStarType;
    private int mSize;
    private long mSectorX;
    private long mSectorY;
    private int mOffsetX;
    private int mOffsetY;
    private Planet[] mPlanets;

    public String getKey() {
        return mKey;
    }
    public String getName() {
        return mName;
    }
    public StarType getStarType() {
        return mStarType;
    }
    public int getSize() {
        return mSize;
    }
    public long getSectorX() {
        return mSectorX;
    }
    public long getSectorY() {
        return mSectorY;
    }
    public int getOffsetX() {
        return mOffsetX;
    }
    public int getOffsetY() {
        return mOffsetY;
    }
    public int getNumPlanets() {
        if (mPlanets == null) {
            return 0;
        } else {
            return mPlanets.length;
        }
    }
    public Planet[] getPlanets() {
        return mPlanets;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mName);
        parcel.writeInt(mStarType.getIndex());
        parcel.writeInt(mSize);
        parcel.writeLong(mSectorX);
        parcel.writeLong(mSectorY);
        parcel.writeInt(mOffsetX);
        parcel.writeInt(mOffsetY);
        parcel.writeParcelableArray(mPlanets, flags);
    }

    protected void populateFromParcel(Parcel parcel) {
        mKey = parcel.readString();
        mName = parcel.readString();
        mStarType = sStarTypes[parcel.readInt()];
        mSize = parcel.readInt();
        mSectorX = parcel.readLong();
        mSectorY = parcel.readLong();
        mOffsetX = parcel.readInt();
        mOffsetY = parcel.readInt();

        Parcelable[] planets = parcel.readParcelableArray(Planet.class.getClassLoader());
        mPlanets = new Planet[planets.length];
        for (int i = 0; i < planets.length; i++) {
            mPlanets[i] = (Planet) planets[i];
        }
    }

    public static final Parcelable.Creator<StarSummary> CREATOR
                = new Parcelable.Creator<StarSummary>() {
        @Override
        public StarSummary createFromParcel(Parcel parcel) {
            StarSummary s = new StarSummary();
            s.populateFromParcel(parcel);
            return s;
        }

        @Override
        public StarSummary[] newArray(int size) {
            return new StarSummary[size];
        }
    };

    public void populateFromProtocolBuffer(Messages.Star pb) {
        mKey = pb.getKey();
        mName = pb.getName();
        mStarType = sStarTypes[pb.getClassification().getNumber()];
        mSize = pb.getSize();
        mSectorX = pb.getSectorX();
        mSectorY = pb.getSectorY();
        mOffsetX = pb.getOffsetX();
        mOffsetY = pb.getOffsetY();

        int numPlanets = pb.getPlanetsCount();
        mPlanets = new Planet[numPlanets];
        for (int i = 0; i < numPlanets; i++) {
            mPlanets[i] = Planet.fromProtocolBuffer(this, pb.getPlanets(i));
        }
    }

    public void toProtocolBuffer(Messages.Star.Builder pb) {
        pb.setKey(mKey);
        pb.setName(mName);
        pb.setClassification(Messages.Star.CLASSIFICATION.valueOf(mStarType.getIndex()));
        pb.setSize(mSize);
        pb.setSectorX(mSectorX);
        pb.setSectorY(mSectorY);
        pb.setOffsetX(mOffsetX);
        pb.setOffsetY(mOffsetY);

        for (int i = 0; i < mPlanets.length; i++) {
            Messages.Planet.Builder planet = Messages.Planet.newBuilder();
            mPlanets[i].toProtocolBuffer(planet);
            pb.addPlanets(planet);
        }
    }

    public static class StarType {
        private int mIndex;
        private String mDisplayName;
        private String mInternalName;
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
}
