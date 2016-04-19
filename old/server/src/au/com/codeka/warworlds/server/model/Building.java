package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.server.data.SqlResult;

public class Building extends BaseBuilding {
    private int mID;
    private int mColonyID;

    public Building() {
    }
    public Building(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mKey = Integer.toString(mID);
        mColonyID = res.getInt("colony_id");
        mColonyKey = Integer.toString(mColonyID);
        mDesignID = res.getString("design_id");
        mLevel = res.getInt("level");
        mNotes = res.getString("notes");
    }
    public Building(Star star, Colony colony, String designID, String notes) {
        mDesignID = designID;
        mColonyID = colony.getID();
        mColonyKey = Integer.toString(mColonyID);
        mLevel = 1;
        mNotes = notes;
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
