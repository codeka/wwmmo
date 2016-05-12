package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseSector;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

public class Sector extends BaseSector {
    private int mID;
    private double mDistanceToCentre;
    private int mNumColonies;

    public Sector() {
    }
    public Sector(long x, long y) {
        mID = 0;
        mX = x;
        mY = y;
        mDistanceToCentre = Math.sqrt((x * x) + (y * y));
        mNumColonies = 0;
    }
    public Sector(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mX = res.getLong("x");
        mY = res.getLong("y");
        mDistanceToCentre = res.getDouble("distance_to_centre");
        mNumColonies = res.getInt("num_colonies");
    }

    public double getDistanceToCentre() {
        return mDistanceToCentre;
    }
    public int getNumColonies() {
        return mNumColonies;
    }

    public int getID() {
        return mID;
    }
    public void setID(int id) {
        mID = id;
    }

    @Override
    protected BaseStar createStar(Messages.Star pb) {
        Star s = new Star();
        if (pb != null) {
            s.fromProtocolBuffer(pb);
        }
        return s;
    }

    @Override
    protected BaseColony createColony(Messages.Colony pb) {
        Colony c = new Colony();
        if (pb != null) {
            c.fromProtocolBuffer(pb);
        }
        return c;
    }

    @Override
    protected BaseFleet createFleet(Messages.Fleet pb) {
        Fleet f = new Fleet();
        if (pb != null) {
            f.fromProtocolBuffer(pb);
        }
        return f;
    }
}
