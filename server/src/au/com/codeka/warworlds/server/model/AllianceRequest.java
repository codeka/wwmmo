package au.com.codeka.warworlds.server.model;

import com.google.api.client.util.Objects;

import java.sql.SQLException;

import au.com.codeka.common.model.BaseAllianceRequest;
import au.com.codeka.common.model.BaseAllianceRequestVote;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.utils.ImageSizer;

public class AllianceRequest extends BaseAllianceRequest {
  public AllianceRequest() {
  }

  public AllianceRequest(SqlResult res) throws SQLException {
    mID = res.getInt("id");
    mAllianceID = res.getInt("alliance_id");
    mRequestEmpireID = res.getInt("request_empire_id");
    mRequestDate = res.getDateTime("request_date");
    mRequestType = RequestType.fromNumber(res.getInt("request_type"));
    mMessage = res.getString("message");
    mState = RequestState.fromNumber(res.getInt("state"));
    mNumVotes = res.getInt("votes");
    mTargetEmpireID = res.getInt("target_empire_id");
    mAmount = res.getFloat("amount");
    mPngImage = res.getBytes("png_image");
    mNewName = res.getString("new_name");
    mNewDescription = res.getString("new_description");
  }

  public void setID(int id) {
    mID = id;
  }

  public void setState(RequestState state) {
    mState = state;
  }

  public void setNewName(String newName) {
    mNewName = newName;
  }

  public void setNewDescription(String newDescription) {
    mNewDescription = newDescription;
  }

  public void ensurePngImageMaxSize(int maxWidth, int maxHeight) {
    if (mPngImage != null) {
      mPngImage = ImageSizer.ensureMaxSize(mPngImage, maxWidth, maxHeight);
    }
  }

  @Override
  protected BaseAllianceRequestVote createVote(Messages.AllianceRequestVote pb) {
    AllianceRequestVote vote = new AllianceRequestVote();
    if (pb != null) {
      vote.fromProtocolBuffer(pb);
    }
    return vote;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("id", mID)
        .add("allianceID", mAllianceID)
        .add("requestEmpireID", mRequestEmpireID)
        .add("requestDate", mRequestDate)
        .add("requestType", mRequestType)
        .add("message", mMessage)
        .add("state", mState)
        .add("numVotes", mNumVotes)
        .add("targetEmpireID", mTargetEmpireID)
        .add("amount", mAmount)
        .add("newName", mNewName)
        .add("newDescription", mNewDescription)
        .toString();
  }
}
