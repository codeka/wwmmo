package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

public class BuildRequest extends BaseBuildRequest {
    private int mID;
    private int mStarID;
    private int mColonyID;
    private int mEmpireID;
    private Integer mExistingBuildingID;
    private boolean mDisableNotification;

    public BuildRequest() {
    }
    public BuildRequest(Star star, SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mKey = Integer.toString(mID);
        mDesignKind = DesignKind.fromNumber(res.getInt("design_kind"));
        mDesignID = res.getString("design_id");
        mColonyID = res.getInt("colony_id");
        mColonyKey = Integer.toString(mColonyID);
        mEndTime = res.getDateTime("end_time");
        mStartTime = res.getDateTime("start_time");
        mProgress = res.getFloat("progress");
        mCount = res.getInt("count");
        mStarID = res.getInt("star_id");
        mStarKey = Integer.toString(mStarID);
        mPlanetIndex = res.getInt("planet_index");
        mEmpireID = res.getInt("empire_id");
        mEmpireKey = Integer.toString(mEmpireID);
        mExistingBuildingID = res.getInt("existing_building_id");
        if (mExistingBuildingID != null) {
            mExistingBuildingKey = Integer.toString(mExistingBuildingID);
            mExistingBuildingLevel = 1;
            for (BaseColony baseColony : star.getColonies()) {
                for (BaseBuilding baseBuilding : baseColony.getBuildings()) {
                    Building building = (Building) baseBuilding;
                    if (building.getID() == mExistingBuildingID) {
                        mExistingBuildingLevel = building.getLevel();
                    }
                }
            }
        }
        mExistingFleetID = res.getInt("existing_fleet_id");
        if (mExistingFleetID != null) {
            mUpgradeID = res.getString("upgrade_id");
        }
        if (res.getInt("disable_notification") != null && res.getInt("disable_notification") > 0) {
            mDisableNotification = true;
        }
        mNotes = res.getString("notes");
    }

    public int getID() {
        return mID;
    }
    public int getStarID() {
        return mStarID;
    }
    public int getColonyID() {
        return mColonyID;
    }
    public int getEmpireID() {
        return mEmpireID;
    }
    public int getExistingBuildingID() {
        return mExistingBuildingID;
    }

    public Design getDesign() {
        return DesignManager.i.getDesign(mDesignKind, mDesignID);
    }

    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(mID);
    }
    public void setCount(int count) {
        mCount = count;
    }
    public void setPlanetIndex(int planetIndex) {
        mPlanetIndex = planetIndex;
    }
    public boolean getDisableNotification() {
        return mDisableNotification;
    }
    public void disableNotification() {
        mDisableNotification = true;
    }

    @Override
    public void fromProtocolBuffer(Messages.BuildRequest pb) {
        super.fromProtocolBuffer(pb);

        if (mKey != null) {
            mID = Integer.parseInt(mKey);
        }
        mStarID = Integer.parseInt(mStarKey);
        mColonyID = Integer.parseInt(mColonyKey);
        mEmpireID = Integer.parseInt(mEmpireKey);
        if (mExistingBuildingKey != null) {
            mExistingBuildingID = Integer.parseInt(mExistingBuildingKey);
        }
    }
}
