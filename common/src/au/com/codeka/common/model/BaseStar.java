package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;


/**
 * A star is \i basically a container for planets. It shows up on the starfield list.
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

    protected abstract BasePlanet createPlanet(Messages.Planet pb);
    protected abstract BaseColony createColony(Messages.Colony pb);
    protected abstract BaseBuilding createBuilding(Messages.Building pb);
    protected abstract BaseEmpirePresence createEmpirePresence(Messages.EmpirePresence pb);
    protected abstract BaseFleet createFleet(Messages.Fleet pb);
    protected abstract BaseBuildRequest createBuildRequest(Messages.BuildRequest pb);
    public abstract BaseCombatReport createCombatReport(Messages.CombatReport pb);
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
        List<BaseEmpirePresence> empires = new ArrayList<BaseEmpirePresence>();
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

    public void fromProtocolBuffer(Messages.Star pb) {
        mKey = pb.getKey();
        mName = pb.getName();
        mStarType = getStarType(Type.values()[pb.getClassification().getNumber()]);
        mSize = pb.getSize();
        mSectorX = pb.getSectorX();
        mSectorY = pb.getSectorY();
        mOffsetX = pb.getOffsetX();
        mOffsetY = pb.getOffsetY();
        if (pb.hasTimeEmptied()) {
            mTimeEmptied = new DateTime(pb.getTimeEmptied() * 1000, DateTimeZone.UTC);
        }

        int numPlanets = pb.getPlanetsCount();
        mPlanets = new BasePlanet[numPlanets];
        for (int i = 0; i < numPlanets; i++) {
            mPlanets[i] = createPlanet(pb.getPlanets(i));
        }
        if (pb.getLastSimulation() == 0) {
            mLastSimulation = null;
        } else {
            mLastSimulation = new DateTime(pb.getLastSimulation() * 1000, DateTimeZone.UTC);
        }

        mColonies = new ArrayList<BaseColony>();
        for(Messages.Colony colony_pb : pb.getColoniesList()) {
            if (colony_pb.getPopulation() < 1.0) {
                // colonies with zero population are dead -- they just don't
                // know it yet.
                continue;
            }
            BaseColony c = createColony(colony_pb);

            for (int i = 0; i < pb.getBuildingsCount(); i++) {
                Messages.Building bpb = pb.getBuildings(i);
                if (bpb.getColonyKey().equals(c.getKey())) {
                    c.getBuildings().add(createBuilding(bpb));
                }
            }

            mColonies.add(c);
        }

        mEmpires = new ArrayList<BaseEmpirePresence>();
        for (Messages.EmpirePresence empirePresencePb : pb.getEmpiresList()) {
            mEmpires.add(createEmpirePresence(empirePresencePb));
        }

        mBuildRequests = new ArrayList<BaseBuildRequest>();
        for (Messages.BuildRequest buildRequestPb : pb.getBuildRequestsList()) {
            mBuildRequests.add(createBuildRequest(buildRequestPb));
        }

        mFleets = new ArrayList<BaseFleet>();
        for (Messages.Fleet fleetPb : pb.getFleetsList()) {
            mFleets.add(createFleet(fleetPb));
        }

        if (pb.hasCurrentCombatReport()) {
            mCombatReport = createCombatReport(pb.getCurrentCombatReport());
        }

        if (pb.hasExtra() && pb.getExtra().hasWormholeEmpireId()) {
            mWormholeExtra = new WormholeExtra();
            mWormholeExtra.fromProtocolBuffer(pb.getExtra());
        }
    }

    public void toProtocolBuffer(Messages.Star.Builder pb) {
        toProtocolBuffer(pb, false);
    }

    /**
     * Convert this star to a protocol buffer. If summary is true, we'll skip a bunch of
     * fields (fleets, build requests, empires, colonies, etc)
     */
    public void toProtocolBuffer(Messages.Star.Builder pb, boolean summary) {
        if (mKey != null) {
            pb.setKey(mKey);
        }
        pb.setName(mName);
        pb.setClassification(Messages.Star.CLASSIFICATION.valueOf(mStarType.getType().ordinal()));
        pb.setSize(mSize);
        pb.setSectorX(mSectorX);
        pb.setSectorY(mSectorY);
        pb.setOffsetX(mOffsetX);
        pb.setOffsetY(mOffsetY);
        if (mLastSimulation != null) {
            pb.setLastSimulation(mLastSimulation.getMillis() / 1000);
        }
        if (mTimeEmptied != null) {
            pb.setTimeEmptied(mTimeEmptied.getMillis() / 1000);
        }

        for (BasePlanet planet : mPlanets) {
            Messages.Planet.Builder planet_pb = Messages.Planet.newBuilder();
            planet.toProtocolBuffer(planet_pb);
            pb.addPlanets(planet_pb);
        }

        if (mColonies != null) for (BaseColony colony : mColonies) {
            Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
            colony.toProtocolBuffer(colony_pb);
            pb.addColonies(colony_pb);
        }

        if (mFleets != null) for (BaseFleet fleet : mFleets) {
            Messages.Fleet.Builder fleet_pb = Messages.Fleet.newBuilder();
            fleet.toProtocolBuffer(fleet_pb);
            pb.addFleets(fleet_pb);
        }

        if (mEmpires != null) for (BaseEmpirePresence empire : mEmpires) {
            Messages.EmpirePresence.Builder empire_pb = Messages.EmpirePresence.newBuilder();
            empire.toProtocolBuffer(empire_pb);
            pb.addEmpires(empire_pb);
        }

        if (!summary) {
            if (mBuildRequests != null) for (BaseBuildRequest buildRequest : mBuildRequests) {
                Messages.BuildRequest.Builder build_request_pb = Messages.BuildRequest.newBuilder();
                buildRequest.toProtocolBuffer(build_request_pb);
                pb.addBuildRequests(build_request_pb);
            }

            if (mColonies != null) for (BaseColony colony : mColonies) {
                for (BaseBuilding building : colony.getBuildings()) {
                    Messages.Building.Builder building_pb = Messages.Building.newBuilder();
                    building.toProtocolBuffer(building_pb);
                    pb.addBuildings(building_pb);
                }
            }

            if (mCombatReport != null) {
                Messages.CombatReport.Builder combat_report_pb = Messages.CombatReport.newBuilder();
                mCombatReport.toProtocolBuffer(combat_report_pb);
                pb.setCurrentCombatReport(combat_report_pb);
            }
        }

        Messages.Star.StarExtra.Builder star_extra_pb = null;
        if (mWormholeExtra != null) {
            star_extra_pb = Messages.Star.StarExtra.newBuilder();
            mWormholeExtra.toProtocolBuffer(star_extra_pb);
        }

        if (star_extra_pb != null) {
            pb.setExtra(star_extra_pb);
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
            mTuneCompleteTime = DateTime.now().plusHours(getTuneTimeHours());

            if (mTuneHistory == null) {
                mTuneHistory = new ArrayList<DateTime>();
            }
            mTuneHistory.add(0, mTuneCompleteTime);
            while (mTuneHistory.size() > 10) {
                mTuneHistory.remove(mTuneHistory.size() - 1);
            }
        }

        public void fromProtocolBuffer(Messages.Star.StarExtra pb) {
            mDestWormholeID = pb.getWormholeDestStarId();
            if (pb.hasWormholeTuneCompleteTime()) {
                mTuneCompleteTime = new DateTime(pb.getWormholeTuneCompleteTime() * 1000);
            }
            if (pb.getWormholeTuneHistoryCount() > 0) {
                mTuneHistory = new ArrayList<DateTime>();
                for (int i = 0; i < pb.getWormholeTuneHistoryCount(); i++) {
                    mTuneHistory.add(new DateTime(pb.getWormholeTuneHistory(i) * 1000));
                }
            }
            mEmpireID = pb.getWormholeEmpireId();
        }

        public void toProtocolBuffer(Messages.Star.StarExtra.Builder pb) {
            pb.setWormholeDestStarId(mDestWormholeID);
            if (mTuneCompleteTime != null) {
                pb.setWormholeTuneCompleteTime(mTuneCompleteTime.getMillis() / 1000);
            }
            if (mTuneHistory != null) for (DateTime dt : mTuneHistory) {
                pb.addWormholeTuneHistory(dt.getMillis() / 1000);
            }
            pb.setWormholeEmpireId(mEmpireID);
        }
    }
}
