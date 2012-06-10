package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Star {
    private static Logger log = LoggerFactory.getLogger(Star.class);
    private Sector mSector;
    private String mKey;
    private String mName;
    private int mColour;
    private int mSize;
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
    public int getColour() {
        return mColour;
    }
    public int getSize() {
        return mSize;
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
        s.mColour = pb.getColour();
        s.mSize = pb.getSize();
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
}
