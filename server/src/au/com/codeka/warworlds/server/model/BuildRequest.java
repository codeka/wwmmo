package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;

public class BuildRequest extends BaseBuildRequest {
    private int mID;
    private int mStarID;
    private int mColonyID;
    private int mEmpireID;
    private int mExistingBuildingID;

    public BuildRequest() {
    }
    public BuildRequest(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mKey = Integer.toString(mID);
        mDesignKind = DesignKind.fromNumber(rs.getInt("design_kind"));
        mDesignID = rs.getString("design_id");
        mColonyID = rs.getInt("colony_id");
        mColonyKey = Integer.toString(mColonyID);
        mEndTime = new DateTime(rs.getTimestamp("end_time").getTime());
        mStartTime = new DateTime(rs.getTimestamp("start_time").getTime());
        mProgress = (float) rs.getDouble("progress");
        mCount = rs.getInt("count");
        mStarID = rs.getInt("star_id");
        mStarKey = Integer.toString(mStarID);
        mPlanetIndex = rs.getInt("planet_index");
        mEmpireID = rs.getInt("empire_id");
        mEmpireKey = Integer.toString(mEmpireID);
        mExistingBuildingID = rs.getInt("existing_building_id");
        if (!rs.wasNull()) {
            mExistingBuildingKey = Integer.toString(mExistingBuildingID);
            mExistingBuildingLevel = 1; // TODO
        }
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
