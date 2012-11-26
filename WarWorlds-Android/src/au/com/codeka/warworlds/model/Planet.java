package au.com.codeka.warworlds.model;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;


public class Planet implements Parcelable {
    private static PlanetType[] sPlanetTypes = {
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

    private StarSummary mStarSummary;
    private int mIndex;
    private PlanetType mPlanetType;
    private int mSize;
    private int mPopulationCongeniality;
    private int mFarmingCongeniality;
    private int mMiningCongeniality;

    public StarSummary getStarSummary() {
        return mStarSummary;
    }
    public void setStarSummary(StarSummary starSummary) {
        mStarSummary = starSummary;
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
        return mStarSummary.getKey().hashCode() ^ (mIndex * 632548);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mIndex);
        parcel.writeInt(mPlanetType.getIndex());
        parcel.writeInt(mSize);
        parcel.writeInt(mPopulationCongeniality);
        parcel.writeInt(mFarmingCongeniality);
        parcel.writeInt(mMiningCongeniality);
    }

    public static final Parcelable.Creator<Planet> CREATOR
                = new Parcelable.Creator<Planet>() {
        @Override
        public Planet createFromParcel(Parcel parcel) {
            Planet p = new Planet();
            p.mIndex = parcel.readInt();
            p.mPlanetType = sPlanetTypes[parcel.readInt()];
            p.mSize = parcel.readInt();
            p.mPopulationCongeniality = parcel.readInt();
            p.mFarmingCongeniality = parcel.readInt();
            p.mMiningCongeniality = parcel.readInt();
            return p;
        }

        @Override
        public Planet[] newArray(int size) {
            return new Planet[size];
        }
    };

    /**
     * Converts the given Planet protocol buffer into a \c Planet.
     */
    public static Planet fromProtocolBuffer(StarSummary starSummary, Messages.Planet pb) {
        Planet p = new Planet();
        p.mStarSummary = starSummary;
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
