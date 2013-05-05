package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;

public class Building extends BaseBuilding {
    private int mID;
    private int mColonyID;

    public Building() {
    }
    public Building(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mKey = Integer.toString(mID);
        mColonyID = rs.getInt("colony_id");
        mColonyKey = Integer.toString(mColonyID);
        mDesignID = rs.getString("design_id");
        mLevel = rs.getInt("level");
    }
    public Building(Star star, Colony colony, String designID) {
        mDesignID = designID;
        mColonyID = colony.getID();
        mColonyKey = Integer.toString(mColonyID);
        mLevel = 1;
    }

    public int getID() {
        return mID;
    }
    public int getColonyID() {
        return mColonyID;
    }

    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(id);
    }
    public void setLevel(int level) {
        mLevel = level;
    }

    public BuildingDesign getDesign() {
        return (BuildingDesign) DesignManager.i.getDesign(DesignKind.BUILDING, mDesignID);
    }
}
