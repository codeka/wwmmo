package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAllianceRequest;

public class AllianceRequest extends BaseAllianceRequest {
    public AllianceRequest() {
    }

    public AllianceRequest(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mAllianceID = rs.getInt("alliance_id");
        mRequestEmpireID = rs.getInt("request_empire_id");
        mRequestDate = new DateTime(rs.getTimestamp("request_date").getTime());
        mRequestType = RequestType.fromNumber(rs.getInt("request_type"));
        mMessage = rs.getString("message");
        mState = RequestState.fromNumber(rs.getInt("state"));
        mVotes = rs.getInt("votes");
        mTargetEmpireID = rs.getInt("target_empire_id");
        if (rs.wasNull()) {
            mTargetEmpireID = null;
        }
        mAmount = rs.getFloat("amount");
        if (rs.wasNull()) {
            mAmount = null;
        }
    }

    public void setID(int id){
        mID = id;
    }

    public void setState(RequestState state) {
        mState = state;
    }
}
