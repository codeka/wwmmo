package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.protobuf.Messages;

public class Alliance extends BaseAlliance {
    private int mID;
    private int mCreatorEmpireID;

    public Alliance() {
        mMembers = new ArrayList<BaseAllianceMember>();
    }
    public Alliance(Integer id, ResultSet rs) throws SQLException {
        if (id == null) {
            mID = rs.getInt("id");
        } else {
            mID = (int) id;
        }
        mKey = Integer.toString(mID);
        try {
            mName = rs.getString("alliance_name");
        } catch (Exception e) {
            mName = rs.getString("name");
        }
        mCreatorEmpireID = rs.getInt("creator_empire_id");
        mCreatorEmpireKey = Integer.toString(mCreatorEmpireID);
        mTimeCreated = new DateTime(rs.getTimestamp("created_date").getTime());
        mNumMembers = rs.getInt("num_empires");
        mBankBalance = rs.getDouble("bank_balance");
        mMembers = new ArrayList<BaseAllianceMember>();
        mDateImageUpdated = new DateTime(rs.getTimestamp("image_updated_date").getTime());
    }

    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(id);
    }
    public int getID() {
        return mID;
    }

    public boolean isEmpireMember(int empireID) {
        for (BaseAllianceMember baseAllianceMember : mMembers) {
            if (Integer.parseInt(baseAllianceMember.getEmpireKey()) == empireID) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BaseAllianceMember createAllianceMember(Messages.AllianceMember pb) {
        AllianceMember am = new AllianceMember();
        if (pb != null) {
            am.fromProtocolBuffer(pb);
        }
        return am;
    }
}
