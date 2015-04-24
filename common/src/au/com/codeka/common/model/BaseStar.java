package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.BuildRequest;
import au.com.codeka.common.protobuf.Building;
import au.com.codeka.common.protobuf.Colony;
import au.com.codeka.common.protobuf.CombatReport;
import au.com.codeka.common.protobuf.EmpirePresence;
import au.com.codeka.common.protobuf.Fleet;
import au.com.codeka.common.protobuf.Planet;
import au.com.codeka.common.protobuf.Star;

/**
 * A star is basically a container for planets. It shows up on the starfield list.
 */
public abstract class BaseStar {

    public enum Type {
        Blue, White, Yellow, Orange, Red, Neutron, BlackHole, Marker, Wormhole,
    }

    protected static StarType[] sStarTypes = {
        new StarType.Builder().setType(Type.Blue)
                              .setDisplayName("Blue")
                              .setInternalName("blue")
                              .setShortName("B")
                              .build(),
        new StarType.Builder().setType(Type.White)
                              .setDisplayName("White")
                              .setInternalName("white")
                              .setShortName("W")
                              .build(),
        new StarType.Builder().setType(Type.Yellow)
                              .setDisplayName("Yellow")
                              .setInternalName("yellow")
                              .setShortName("Y")
                              .build(),
        new StarType.Builder().setType(Type.Orange)
                              .setDisplayName("Orange")
                              .setInternalName("orange")
                              .setShortName("O")
                              .build(),
        new StarType.Builder().setType(Type.Red)
                              .setDisplayName("Red")
                              .setInternalName("red")
                              .setShortName("R")
                              .build(),
        new StarType.Builder().setType(Type.Neutron)
                              .setDisplayName("Neutron")
                              .setInternalName("neutron")
                              .setShortName("N")
                              .setBaseSize(1.0)
                              .setImageScale(4.0)
                              .build(),
        new StarType.Builder().setType(Type.BlackHole)
                              .setDisplayName("Black Hole")
                              .setInternalName("black-hole")
                              .setShortName("BH")
                              .build(),
        new StarType.Builder().setType(Type.Marker)
                              .setDisplayName("Marker")
                              .setInternalName("marker")
                              .setShortName("M")
                              .build(),
        new StarType.Builder().setType(Type.Wormhole)
                              .setDisplayName("Wormhole")
                              .setInternalName("wormhole")
                              .setShortName("WH")
                              .setImageScale(2.0)
                              .build()
    };

    public static StarType getStarType(Type type) {
        return sStarTypes[type.ordinal()];
    }
    public static StarType[] getStarTypes() {
        return sStarTypes;
    }

    protected String mKey;
    protected String mName;
    protected StarType mStarType;
    protected int mSize;
    protected long mSectorX;
    protected long mSectorY;
    protected int mOffsetX;
    protected int mOffsetY;
    protected BasePlanet[] mPlanets;
    protected ArrayList<BaseColony> mColonies;
    protected ArrayList<BaseEmpirePresence> mEmpires;
    protected ArrayList<BaseFleet> mFleets;
    protected ArrayList<BaseBuildRequest> mBuildRequests;
    protected DateTime mLastSimulation;
    protected DateTime mTimeEmptied;
    protected BaseCombatReport mCombatReport;
    protected WormholeExtra mWormholeExtra;

    protected BaseStar() {
        mColonies = null;
        mEmpires = null;
        mFleets = null;
        mBuildRequests = null;
    }

    protected abstract BasePlanet createPlanet(Planet pb);
    protected abstract BaseColony createColony(Colony pb);
    protected abstract BaseBuilding createBuilding(Building pb);
    protected abstract BaseEmpirePresence createEmpirePresence(EmpirePresence pb);
    protected abstract BaseFleet createFleet(Fleet pb);
    protected abstract BaseBuildRequest createBuildRequest(BuildRequest pb);
    public abstract BaseCombatReport createCombatReport(CombatReport pb);
    public abstract BaseStar clone();

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
    public BasePlanet[] getPlanets() {
        return mPlanets;
    }
    public DateTime getLastSimulation() {
        return mLastSimulation;
    }
    public void setLastSimulation(DateTime dt) {
        mLastSimulation = dt;
    }
    public DateTime getTimeEmptied() {
        return mTimeEmptied;
    }
    public WormholeExtra getWormholeExtra() {
        return mWormholeExtra;
    }

    public List<BaseColony> getColonies() {
        return mColonies;
    }
    public List<BaseEmpirePresence> getEmpirePresences() {
        return mEmpires;
    }
    public List<BaseEmpirePresence> getEmpires() {
        List<BaseEmpirePresence> empires = new ArrayList<>();
        // note: we make sure there's actually a colony for that empire before we return it
        for (BaseEmpirePresence empire : mEmpires) {
            for (BaseColony colony : mColonies) {
                if (colony.getEmpireKey() != null && colony.getEmpireKey().equals(empire.getEmpireKey())) {
                    empires.add(empire);
                    break;
                }
            }
        }

        return empires;
    }
    public BaseEmpirePresence getEmpire(String empireKey) {
        for (BaseEmpirePresence ep : mEmpires) {
            if (ep.getEmpireKey().equals(empireKey)) {
                return ep;
            }
        }
        return null;
    }
    public List<BaseFleet> getFleets() {
        return mFleets;
    }
    public BaseFleet getFleet(int fleetID) {
        for (BaseFleet fleet : mFleets) {
            if (Integer.parseInt(fleet.getKey()) == fleetID) {
                return fleet;
            }
        }
        return null;
    }
    public List<BaseBuildRequest> getBuildRequests() {
        return mBuildRequests;
    }

    public void addColony(BaseColony colony) {
        if (mColonies == null) {
            mColonies = new ArrayList<BaseColony>();
        }
        mColonies.add(colony);
    }

    public void addFleet(BaseFleet fleet) {
        if (mFleets == null) {
            mFleets = new ArrayList<BaseFleet>();
        }
        mFleets.add(fleet);
    }

    public BaseFleet findFleet(String fleetKey) {
        for (BaseFleet f : mFleets) {
            if (f.getKey().equals(fleetKey)) {
                return f;
            }
        }
        return null;
    }

    public void setName(String name) {
        mName = name;
    }

    public BaseCombatReport getCombatReport() {
        return mCombatReport;
    }
    public void setCombatReport(BaseCombatReport report) {
        mCombatReport = report;
    }

    public void fromProtocolBuffer(Star pb) {
        mKey = pb.key;
        mName = pb.name;
        mStarType = getStarType(Type.values()[pb.classification.getValue()]);
        mSize = pb.size;
        mSectorX = pb.sector_x;
        mSectorY = pb.sector_y;
        mOffsetX = pb.offset_x;
        mOffsetY = pb.offset_y;
        if (pb.time_emptied != null) {
            mTimeEmptied = new DateTime(pb.time_emptied * 1000, DateTimeZone.UTC);
        }

        int numPlanets = pb.planets == null ? 0 : pb.planets.size();
        mPlanets = new BasePlanet[numPlanets];
        for (int i = 0; i < numPlanets; i++) {
            mPlanets[i] = createPlanet(pb.planets.get(i));
        }
        if (pb.last_simulation != null) {
            mLastSimulation = new DateTime(pb.last_simulation * 1000, DateTimeZone.UTC);
        }

        mColonies = new ArrayList<>();
        for(Colony colony_pb : pb.colonies) {
            if (colony_pb.population < 1.0) {
                // colonies with zero population are dead -- they just don't
                // know it yet.
                continue;
            }
            BaseColony c = createColony(colony_pb);

            for (int i = 0; i < pb.buildings.size(); i++) {
                Building bpb = pb.buildings.get(i);
                if (bpb.colony_key.equals(c.getKey())) {
                    c.getBuildings().add(createBuilding(bpb));
                }
            }

            mColonies.add(c);
        }

        mEmpires = new ArrayList<>();
        for (EmpirePresence empirePresencePb : pb.empires) {
            mEmpires.add(createEmpirePresence(empirePresencePb));
        }

        mBuildRequests = new ArrayList<>();
        for (BuildRequest buildRequestPb : pb.build_requests) {
            mBuildRequests.add(createBuildRequest(buildRequestPb));
        }

        mFleets = new ArrayList<>();
        for (Fleet fleetPb : pb.fleets) {
            mFleets.add(createFleet(fleetPb));
        }

        if (pb.current_combat_report != null) {
            mCombatReport = createCombatReport(pb.current_combat_report);
        }

        if (pb.extra != null && pb.extra.wormhole_empire_id != null) {
            mWormholeExtra = new WormholeExtra();
            mWormholeExtra.fromProtocolBuffer(pb.extra);
        }
    }

    public void toProtocolBuffer(Star pb) {
        toProtocolBuffer(pb, false);
    }

    /**
     * Convert this star to a protocol buffer. If summary is true, we'll skip a bunch of
     * fields (fleets, build requests, empires, colonies, etc)
     */
    public void toProtocolBuffer(Star pb, boolean summary) {
        pb.key = mKey;
        pb.name = mName;
        pb.classification = Star.CLASSIFICATION.values()[mStarType.getType().ordinal()];
        pb.size = mSize;
        pb.sector_x = mSectorX;
        pb.sector_y = mSectorY;
        pb.offset_x = mOffsetX;
        pb.offset_y = mOffsetY;
        if (mLastSimulation != null) {
            pb.last_simulation = mLastSimulation.getMillis() / 1000;
        }
        if (mTimeEmptied != null) {
            pb.time_emptied = mTimeEmptied.getMillis() / 1000;
        }

        pb.planets = new ArrayList<>();
        for (BasePlanet planet : mPlanets) {
            Planet planet_pb = new Planet();
            planet.toProtocolBuffer(planet_pb);
            pb.planets.add(planet_pb);
        }

        pb.colonies = new ArrayList<>();
        if (mColonies != null) for (BaseColony colony : mColonies) {
            Colony colony_pb = new Colony();
            colony.toProtocolBuffer(colony_pb);
            pb.colonies.add(colony_pb);
        }

        pb.fleets = new ArrayList<>();
        if (mFleets != null) for (BaseFleet fleet : mFleets) {
            Fleet fleet_pb = new Fleet();
            fleet.toProtocolBuffer(fleet_pb);
            pb.fleets.add(fleet_pb);
        }

        pb.empires = new ArrayList<>();
        if (mEmpires != null) for (BaseEmpirePresence empire : mEmpires) {
            EmpirePresence empire_pb = new EmpirePresence();
            empire.toProtocolBuffer(empire_pb);
            pb.empires.add(empire_pb);
        }

        if (!summary) {
            pb.build_requests = new ArrayList<>();
            if (mBuildRequests != null) for (BaseBuildRequest buildRequest : mBuildRequests) {
                BuildRequest build_request_pb = new BuildRequest();
                buildRequest.toProtocolBuffer(build_request_pb);
                pb.build_requests.add(build_request_pb);
            }

            pb.buildings = new ArrayList<>();
            if (mColonies != null) for (BaseColony colony : mColonies) {
                for (BaseBuilding building : colony.getBuildings()) {
                    Building building_pb = new Building();
                    building.toProtocolBuffer(building_pb);
                    pb.buildings.add(building_pb);
                }
            }

            if (mCombatReport != null) {
                CombatReport combat_report_pb = new CombatReport();
                mCombatReport.toProtocolBuffer(combat_report_pb);
                pb.current_combat_report = combat_report_pb;
            }
        }

        Star.StarExtra star_extra_pb = null;
        if (mWormholeExtra != null) {
            star_extra_pb = new Star.StarExtra();
            mWormholeExtra.toProtocolBuffer(star_extra_pb);
        }

        if (star_extra_pb != null) {
            pb.extra = star_extra_pb;
        }
    }

    public static class StarType {
        private Type mType;
        private String mDisplayName;
        private String mInternalName;
        private String mShortName;
        private double mBaseSize;
        private double mImageScale;

        public Type getType() {
            return mType;
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

            public Builder setType(Type type) {
                mStarType.mType = type;
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

    public static class WormholeExtra {
        private int mDestWormholeID;
        private DateTime mTuneCompleteTime;
        private List<DateTime> mTuneHistory;
        private int mEmpireID;

        public WormholeExtra() {
        }
        public WormholeExtra(int empireID) {
            mEmpireID = empireID;
        }

        public int getDestWormholeID() {
            return mDestWormholeID;
        }
        public DateTime getTuneCompleteTime() {
            return mTuneCompleteTime;
        }
        public List<DateTime> getTuneHistory() {
            return mTuneHistory;
        }
        public int getEmpireID() {
            return mEmpireID;
        }

        public int getTuneTimeHours() {
            if (mTuneHistory == null || mTuneHistory.size() == 0) {
                return 0;
            } else {
                DateTime twoWeeksAgo = DateTime.now().minusWeeks(2);
                int numTunes = 0;
                for (DateTime dt : mTuneHistory) {
                    if (dt.isAfter(twoWeeksAgo)) {
                        numTunes ++;
                    }
                }
                return (int) Math.pow(numTunes + 1, 2);
            }
        }

        public void tuneTo(int destWormholeID) {
            mDestWormholeID = destWormholeID;
            if (destWormholeID != 0) {
                mTuneCompleteTime = DateTime.now().plusHours(getTuneTimeHours());

                if (mTuneHistory == null) {
                    mTuneHistory = new ArrayList<>();
                }
                mTuneHistory.add(0, mTuneCompleteTime);
                while (mTuneHistory.size() > 10) {
                    mTuneHistory.remove(mTuneHistory.size() - 1);
                }
            }
        }

        /*
         * Update the owning empire of this wormhole to the given empire. This will also reset
         * the wormhole so it's not tuned to anything any more. It does not clear out the tune
         * history, however, which means re-tuning may take some time for the new owner.
         */
        public void setEmpireID(int empireID) {
            mEmpireID = empireID;
            mDestWormholeID = 0;
            mTuneCompleteTime = null;
        }

        public void fromProtocolBuffer(Star.StarExtra pb) {
            mDestWormholeID = pb.wormhole_dest_star_id;
            if (pb.wormhole_tune_complete_time != null) {
                mTuneCompleteTime = new DateTime(pb.wormhole_tune_complete_time * 1000);
            }
            if (pb.wormhole_tune_history != null) {
                mTuneHistory = new ArrayList<>();
                for (int i = 0; i < pb.wormhole_tune_history.size(); i++) {
                    mTuneHistory.add(new DateTime(pb.wormhole_tune_history.get(i) * 1000));
                }
            }
            mEmpireID = pb.wormhole_empire_id;
        }

        public void toProtocolBuffer(Star.StarExtra pb) {
            pb.wormhole_dest_star_id = mDestWormholeID;
            if (mTuneCompleteTime != null) {
                pb.wormhole_tune_complete_time = mTuneCompleteTime.getMillis() / 1000;
            }
            pb.wormhole_tune_history = new ArrayList<>();
            if (mTuneHistory != null) for (DateTime dt : mTuneHistory) {
                pb.wormhole_tune_history.add(dt.getMillis() / 1000);
            }
            pb.wormhole_empire_id = mEmpireID;
        }
    }
}
