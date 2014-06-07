package au.com.codeka.warworlds.server.model;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseCombatReport;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

public class Star extends BaseStar {
    private int mID;
    private int mSectorID;
    private ArrayList<ScoutReport> mScoutReports = new ArrayList<ScoutReport>();

    public Star() {
    }
    public Star(Sector sector, int x, int y, int starTypeID, String name, int size) {
        mID = 0;
        mSectorID = sector.getID();
        mSectorX = sector.getX();
        mSectorY = sector.getY();
        mOffsetX = x;
        mOffsetY = y;
        mName = name;
        mSize = size;
        mStarType = sStarTypes[starTypeID];
        mColonies = new ArrayList<BaseColony>();
        mFleets = new ArrayList<BaseFleet>();
    }
    public Star(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mKey = Integer.toString(mID);
        mSectorID = res.getInt("sector_id");
        mSectorX = res.getLong("sector_x");
        mSectorY = res.getLong("sector_y");
        mOffsetX = res.getInt("x");
        mOffsetY = res.getInt("y");
        mName = res.getString("name");
        mSize = res.getInt("size");
        mStarType = sStarTypes[res.getInt("star_type")];
        mLastSimulation = res.getDateTime("last_simulation");
        mTimeEmptied = res.getDateTime("time_emptied");

        try {
            Messages.Planets planets_pb = Messages.Planets.parseFrom(res.getBytes("planets"));
            mPlanets = new BasePlanet[planets_pb.getPlanetsCount()];
            for (int i = 0; i < planets_pb.getPlanetsCount(); i++) {
                mPlanets[i] = new Planet();
                mPlanets[i].fromProtocolBuffer(this, planets_pb.getPlanets(i));
            }
        } catch (IOException e) {
        }

        try {
            byte[] extra = res.getBytes("extra");
            if (extra != null) {
                Messages.Star.StarExtra star_extra_pb = Messages.Star.StarExtra.parseFrom(extra);
                if (star_extra_pb.hasWormholeEmpireId()) {
                    mWormholeExtra = new WormholeExtra();
                    mWormholeExtra.fromProtocolBuffer(star_extra_pb);
                }
            }
        } catch (IOException e) {
        }
    }

    public int getSectorID() {
        return mSectorID;
    }
    public int getID() {
        return mID;
    }
    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(mID);
    }

    public Colony getColony(int id) {
        for (BaseColony colony : mColonies) {
            Colony c = (Colony) colony;
            if (c.getID() == id) {
                return c;
            }
        }
        return null;
    }

    public void setColonies(ArrayList<BaseColony> colonies) {
        mColonies = colonies;
    }
    public void setFleets(ArrayList<BaseFleet> fleets) {
        mFleets = fleets;
    }
    public void setEmpires(ArrayList<BaseEmpirePresence> empires) {
        mEmpires = empires;
    }
    public void setBuildRequests(ArrayList<BaseBuildRequest> buildRequests) {
        mBuildRequests = buildRequests;
    }
    public void setPlanets(Planet[] planets) {
        mPlanets = planets;
    }
    public void setStarType(StarType type) {
        mStarType = type;
    }
    public void setWormholeExtra(WormholeExtra extra) {
        mWormholeExtra = extra;
    }

    public ArrayList<ScoutReport> getScoutReports() {
        return mScoutReports;
    }

    @Override
    protected BasePlanet createPlanet(Messages.Planet pb) {
        Planet p = new Planet();
        if (pb != null) {
            p.fromProtocolBuffer(this, pb);
        }
        return p;
    }

    @Override
    protected BaseColony createColony(Messages.Colony pb) {
        Colony c = new Colony();
        if (pb != null) {
            c.fromProtocolBuffer( pb);
        }
        return c;
    }

    @Override
    protected BaseBuilding createBuilding(Messages.Building pb) {
        Building b = new Building();
        if (pb != null) {
            b.fromProtocolBuffer( pb);
        }
        return b;
    }

    @Override
    protected BaseEmpirePresence createEmpirePresence(Messages.EmpirePresence pb) {
        EmpirePresence ep = new EmpirePresence();
        if (pb != null) {
            ep.fromProtocolBuffer( pb);
        }
        return ep;
    }

    @Override
    protected BaseFleet createFleet(Messages.Fleet pb) {
        Fleet f = new Fleet();
        if (pb != null) {
            f.fromProtocolBuffer( pb);
        }
        return f;
    }

    @Override
    protected BaseBuildRequest createBuildRequest(Messages.BuildRequest pb) {
        BuildRequest br = new BuildRequest();
        if (pb != null) {
            br.fromProtocolBuffer( pb);
        }
        return br;
    }

    @Override
    public BaseCombatReport createCombatReport(Messages.CombatReport pb) {
        CombatReport report = new CombatReport();
        if (pb != null) {
            report.fromProtocolBuffer(pb);
        }
        report.setStarID(mID);
        return report;
    }

    @Override
    public BaseStar clone() {
        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        toProtocolBuffer(star_pb);

        Star clone = new Star();
        clone.fromProtocolBuffer(star_pb.build());
        return clone;
    }
}
