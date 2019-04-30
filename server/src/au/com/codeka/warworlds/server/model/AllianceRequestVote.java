package au.com.codeka.warworlds.server.model;

import org.joda.time.DateTime;

import java.sql.SQLException;

import au.com.codeka.common.model.BaseAllianceRequestVote;
import au.com.codeka.warworlds.server.data.SqlResult;

public class AllianceRequestVote extends BaseAllianceRequestVote {
    public AllianceRequestVote() {
    }
    public AllianceRequestVote(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mAllianceID = res.getInt("alliance_id");
        mAllianceRequestID = res.getInt("alliance_request_id");
        mEmpireID = res.getInt("empire_id");
        mVotes = res.getInt("votes");
        mDate = res.getDateTime("date");
    }
    public AllianceRequestVote(int allianceId, int requestId, int empireId) {
        mAllianceID = allianceId;
        mAllianceRequestID = requestId;
        mEmpireID = empireId;
        mVotes = 9999;
        mDate = DateTime.now();
    }

    public void setEmpireID(int empireID) {
        mEmpireID = empireID;
    }

    public void setVotes(int votes) {
        mVotes = votes;
    }
}
