package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAllianceMember;

public class AllianceMember extends BaseAllianceMember {
    private int mEmpireID;
    private int mAllianceID;

    public AllianceMember() {
    }
    public AllianceMember(ResultSet rs) throws SQLException {
        mEmpireID = rs.getInt("id");
        mEmpireKey = Integer.toString(mEmpireID);
        mAllianceID = rs.getInt("alliance_id");
        mAllianceKey = Integer.toString(mAllianceID);
        mTimeJoined = DateTime.now(); // hrm
        mRank = Rank.fromNumber(rs.getInt("alliance_rank"));
    }
}
