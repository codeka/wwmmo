package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAllianceJoinRequest;

public class AllianceJoinRequest extends BaseAllianceJoinRequest {
    public AllianceJoinRequest() {
    }
    public AllianceJoinRequest(ResultSet rs) throws SQLException {
        mKey = Integer.toString(rs.getInt("id"));
        mAllianceKey = Integer.toString(rs.getInt("alliance_id"));
        mEmpireKey = Integer.toString(rs.getInt("empire_id"));
        mMessage = rs.getString("message");
        mTimeRequested = new DateTime(rs.getTimestamp("request_date").getTime());
        mRequestState = RequestState.fromNumber(rs.getInt("state"));
    }
}
