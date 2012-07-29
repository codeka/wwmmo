package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * A star is \i basically a container for planets. It shows up on the starfield list.
 */
public class Star implements Parcelable {
    private static StarType[] sStarTypes = {
        new StarType.Builder().setIndex(0)
                              .setDisplayName("Blue")
                              .setBitmapBasePath("stars/blue")
                              .build(),
        new StarType.Builder().setIndex(1)
                              .setDisplayName("White")
                              .setBitmapBasePath("stars/white")
                              .build(),
        new StarType.Builder().setIndex(2)
                              .setDisplayName("Yellow")
                              .setBitmapBasePath("stars/yellow")
                              .build(),
        new StarType.Builder().setIndex(3)
                              .setDisplayName("Orange")
                              .setBitmapBasePath("stars/orange")
                              .build(),
        new StarType.Builder().setIndex(4)
                              .setDisplayName("Red")
                              .setBitmapBasePath("stars/red")
                              .build(),
        new StarType.Builder().setIndex(5)
                              .setDisplayName("Neutron")
                              .setBitmapBasePath("stars/neutron")
                              .setBaseSize(1.0)
                              .setImageScale(4.0)
                              .build(),
        new StarType.Builder().setIndex(6)
                              .setDisplayName("Back Hole")
                              .setBitmapBasePath("stars/black-hole")
                              .build()
    };

    private static Logger log = LoggerFactory.getLogger(Star.class);
    private Sector mSector;
    private String mKey;
    private String mName;
    private StarType mStarType;
    private int mSize;
    private long mSectorX;
    private long mSectorY;
    private int mOffsetX;
    private int mOffsetY;
    private int mNumPlanets;
    private Planet[] mPlanets;
    private ArrayList<Colony> mColonies;
    private ArrayList<EmpirePresence> mEmpires;
    private ArrayList<Fleet> mFleets;
    private ArrayList<BuildRequest> mBuildRequests;

    public Star() {
        mSector = null; // can be null if we're fetched separately from the sector
        mPlanets = null; // can be null if planets have not been populated...
        mColonies = null;
        mEmpires = null;
        mFleets = null;
        mBuildRequests = null;
    }

    public Sector getSector() {
        return mSector;
    }
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
            return mNumPlanets;
        } else {
            return mPlanets.length;
        }
    }
    public Planet[] getPlanets() {
        return mPlanets;
    }
    public List<Colony> getColonies() {
        return mColonies;
    }
    public List<EmpirePresence> getEmpires() {
        return mEmpires;
    }
    public EmpirePresence getEmpire(String empireKey) {
        for (EmpirePresence ep : mEmpires) {
            if (ep.getEmpireKey().equals(empireKey)) {
                return ep;
            }
        }
        return null;
    }
    public List<Fleet> getFleets() {
        return mFleets;
    }
    public List<BuildRequest> getBuildRequests() {
        return mBuildRequests;
    }

    public void addColony(Colony colony) {
        if (mColonies == null) {
            mColonies = new ArrayList<Colony>();
        }
        mColonies.add(colony);
    }

    public void setDummySector(long sectorX, long sectorY) {
        mSector = new Sector.DummySector(sectorX, sectorY);
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
        parcel.writeInt(mNumPlanets);
        parcel.writeParcelableArray(mPlanets, flags);
    }

    public static final Parcelable.Creator<Star> CREATOR
                = new Parcelable.Creator<Star>() {
        @Override
        public Star createFromParcel(Parcel parcel) {
            Star s = new Star();
            s.mKey = parcel.readString();
            s.mName = parcel.readString();
            s.mStarType = sStarTypes[parcel.readInt()];
            s.mSize = parcel.readInt();
            s.mSectorX = parcel.readLong();
            s.mSectorY = parcel.readLong();
            s.mOffsetX = parcel.readInt();
            s.mOffsetY = parcel.readInt();
            s.mNumPlanets = parcel.readInt();

            Parcelable[] planets = parcel.readParcelableArray(Planet.class.getClassLoader());
            s.mPlanets = new Planet[planets.length];
            for (int i = 0; i < planets.length; i++) {
                s.mPlanets[i] = (Planet) planets[i];
            }

            return s;
        }

        @Override
        public Star[] newArray(int size) {
            return new Star[size];
        }
    };

    public static Star fromProtocolBuffer(warworlds.Warworlds.Star pb) {
        return fromProtocolBuffer(null, pb);
    }

    /**
     * Converts the given Star protocol buffer into a \c Star.
     */
    public static Star fromProtocolBuffer(Sector sector, warworlds.Warworlds.Star pb) {
        Star s = new Star();
        s.mSector = sector;
        s.mKey = pb.getKey();
        s.mName = pb.getName();
        s.mStarType = sStarTypes[pb.getClassification().getNumber()];
        s.mSize = pb.getSize();
        s.mSectorX = pb.getSectorX();
        s.mSectorY = pb.getSectorY();
        s.mOffsetX = pb.getOffsetX();
        s.mOffsetY = pb.getOffsetY();
        s.mNumPlanets = pb.getNumPlanets();

        int numPlanets = pb.getPlanetsCount();
        if (numPlanets > 0) {
            s.mPlanets = new Planet[numPlanets];
            for (int i = 0; i < numPlanets; i++) {
                s.mPlanets[i] = Planet.fromProtocolBuffer(s, pb.getPlanets(i));
            }
        }

        s.mColonies = new ArrayList<Colony>();
        for(warworlds.Warworlds.Colony colony_pb : pb.getColoniesList()) {
            Colony c = Colony.fromProtocolBuffer(colony_pb);

            for (int i = 0; i < pb.getBuildingsCount(); i++) {
                warworlds.Warworlds.Building bpb = pb.getBuildings(i);
                if (bpb.getColonyKey().equals(c.getKey())) {
                    log.info("Adding building: " + bpb.getDesignName());
                    c.getBuildings().add(Building.fromProtocolBuffer(bpb));
                }
            }

            s.mColonies.add(c);
        }

        s.mEmpires = new ArrayList<EmpirePresence>();
        for (warworlds.Warworlds.EmpirePresence empirePresencePb : pb.getEmpiresList()) {
            s.mEmpires.add(EmpirePresence.fromProtocolBuffer(empirePresencePb));
        }

        s.mBuildRequests = new ArrayList<BuildRequest>();
        for (warworlds.Warworlds.BuildRequest buildRequestPb : pb.getBuildRequestsList()) {
            s.mBuildRequests.add(BuildRequest.fromProtocolBuffer(buildRequestPb));
        }

        s.mFleets = new ArrayList<Fleet>();
        for (warworlds.Warworlds.Fleet fleetPb : pb.getFleetsList()) {
            s.mFleets.add(Fleet.fromProtocolBuffer(fleetPb));
        }

        return s;
    }

    public static class StarType {
        private int mIndex;
        private String mDisplayName;
        private String mBitmapBasePath;
        private double mBaseSize;
        private double mImageScale;

        public int getIndex() {
            return mIndex;
        }
        public String getDisplayName() {
            return mDisplayName;
        }
        public String getBitmapBasePath() {
            return mBitmapBasePath;
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

            public Builder setBitmapBasePath(String path) {
                mStarType.mBitmapBasePath = path;
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
