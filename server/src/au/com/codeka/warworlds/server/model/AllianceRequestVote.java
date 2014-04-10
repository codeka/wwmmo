package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.model.BaseAllianceRequestVote;

public class AllianceRequestVote extends BaseAllianceRequestVote {
    public AllianceRequestVote() {
    }
    public AllianceRequestVote(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mAllianceID = rs.getInt("alliance_id");
        mAllianceRequestID = rs.getInt("alliance_request_id");
        mEmpireID = rs.getInt("empire_id");
        mVotes = rs.getInt("votes");
        mDate = new DateTime(rs.getTimestamp("date").getTime() * 1000, DateTimeZone.UTC);
    }

    public void setEmpireID(int empireID) {
        mEmpireID = empireID;
    }

    public void setVotes(int votes) {
        mVotes = votes;
    }
}
