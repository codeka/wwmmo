package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.warworlds.server.data.SqlResult;

public class AllianceMember extends BaseAllianceMember {
    private int mEmpireID;
    private int mAllianceID;

    public AllianceMember() {
    }
    public AllianceMember(SqlResult res) throws SQLException {
        mEmpireID = res.getInt("id");
        mEmpireKey = Integer.toString(mEmpireID);
        mAllianceID = res.getInt("alliance_id");
        mAllianceKey = Integer.toString(mAllianceID);
        mTimeJoined = DateTime.now(); // hrm
        if (res.getInt("alliance_rank") == null) {
            mRank = Rank.CAPTAIN;
        } else {
            mRank = Rank.fromNumber(res.getInt("alliance_rank"));
        }
    }
}
