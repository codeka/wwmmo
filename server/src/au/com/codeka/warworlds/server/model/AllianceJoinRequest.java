package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAllianceJoinRequest;
import au.com.codeka.common.protobuf.Messages;

public class AllianceJoinRequest extends BaseAllianceJoinRequest {
    int mID;
    int mEmpireID;
    int mAllianceID;

    public AllianceJoinRequest() {
    }
    public AllianceJoinRequest(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mKey = Integer.toString(mID);
        mAllianceID = rs.getInt("alliance_id");
        mAllianceKey = Integer.toString(mAllianceID);
        mEmpireID = rs.getInt("empire_id");
        mEmpireKey = Integer.toString(mEmpireID);
        mMessage = rs.getString("message");
        mTimeRequested = new DateTime(rs.getTimestamp("request_date").getTime());
        mRequestState = RequestState.fromNumber(rs.getInt("state"));
    }

    public int getID() {
        return mID;
    }
    public int getEmpireID() {
        return mEmpireID;
    }
    public int getAllianceID() {
        return mAllianceID;
    }

    @Override
    public void fromProtocolBuffer(Messages.AllianceJoinRequest pb) {
        super.fromProtocolBuffer(pb);
        if (pb.hasKey()) {
            mID = Integer.parseInt(pb.getKey());
        }
        if (pb.hasEmpireKey()) {
            mEmpireID = Integer.parseInt(pb.getEmpireKey());
        }
        if (pb.hasAllianceKey()) {
            mAllianceID = Integer.parseInt(pb.getAllianceKey());
        }
    }
}
