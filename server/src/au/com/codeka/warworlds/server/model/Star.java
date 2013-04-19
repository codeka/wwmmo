package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;

public class Star extends BaseStar {
    private int mID;
    private int mSectorID;

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
    }
    public Star(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mKey = Integer.toString(mID);
        mSectorID = rs.getInt("sector_id");
        mSectorX = rs.getLong("sector_x");
        mSectorY = rs.getLong("sector_y");
        mOffsetX = rs.getInt("x");
        mOffsetY = rs.getInt("y");
        mName = rs.getString("name");
        mSize = rs.getInt("size");
        mStarType = sStarTypes[rs.getInt("star_type")];
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
    public BaseStar clone() {
        return null; //TODO
    }
}
