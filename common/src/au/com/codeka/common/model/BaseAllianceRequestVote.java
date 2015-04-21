package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.AllianceRequestVote;

public class BaseAllianceRequestVote {
    protected int mID;
    protected int mAllianceID;
    protected int mAllianceRequestID;
    protected int mEmpireID;
    protected int mVotes;
    protected DateTime mDate;

    public int getID() {
        return mID;
    }
    public int getAllianceID() {
        return mAllianceID;
    }
    public int getAllianceRequestID() {
        return mAllianceRequestID;
    }
    public int getEmpireID() {
        return mEmpireID;
    }
    public int getVotes() {
        return mVotes;
    }
    public DateTime getDate() {
        return mDate;
    }

    public void fromProtocolBuffer(AllianceRequestVote pb) {
        mID = pb.id;
        mAllianceID = pb.alliance_id;
        mAllianceRequestID = pb.alliance_request_id;
        mEmpireID = pb.empire_id;
        mVotes = pb.votes;
        mDate = new DateTime(pb.date * 1000, DateTimeZone.UTC);
    }

    public void toProtocolBuffer(AllianceRequestVote.Builder pb) {
        pb.id = mID;
        pb.alliance_id = mAllianceID;
        pb.alliance_request_id = mAllianceRequestID;
        pb.empire_id = mEmpireID;
        pb.votes = mVotes;
        pb.date = mDate.getMillis() / 1000;
    }
}
