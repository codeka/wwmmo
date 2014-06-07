package au.com.codeka.warworlds.server.model;

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
    }

    public void setID(int id){
        mID = id;
    }

    public void setState(RequestState state) {
        mState = state;
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
}
