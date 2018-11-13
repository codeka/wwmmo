package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;
import java.util.ArrayList;

import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

public class Alliance extends BaseAlliance {
  private int mID;
  private int mCreatorEmpireID;

  public Alliance() {
    mMembers = new ArrayList<>();
  }

  public Alliance(Integer id, SqlResult res) throws SQLException {
    if (id == null) {
      mID = res.getInt("id");
    } else {
      mID = id;
    }
    mKey = Integer.toString(mID);
    try {
      mName = res.getString("alliance_name");
    } catch (Exception e) {
      mName = res.getString("name");
    }
    mDescription = res.getString("description");
    mCreatorEmpireID = res.getInt("creator_empire_id");
    mCreatorEmpireKey = Integer.toString(mCreatorEmpireID);
    mTimeCreated = res.getDateTime("created_date");
    mNumMembers = res.getInt("num_empires");
    mBankBalance = res.getDouble("bank_balance");
    mMembers = new ArrayList<>();
    mDateImageUpdated = res.getDateTime("image_updated_date");
    mNumPendingRequests = res.getInt("num_pending_requests");
    mIsActive = res.getInt("is_active") != 0;
    mTotalStars = res.getLong("total_stars");
  }

  public void setID(int id) {
    mID = id;
    mKey = Integer.toString(id);
  }

  public int getID() {
    return mID;
  }

  public void setName(String name) {
    mName = name;
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
