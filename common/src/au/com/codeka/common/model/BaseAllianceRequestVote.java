package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

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

    public void fromProtocolBuffer(Messages.AllianceRequestVote pb) {
        mID = pb.getId();
        mAllianceID = pb.getAllianceId();
        mAllianceRequestID = pb.getAllianceRequestId();
        if (pb.hasEmpireId()) {
            mEmpireID = pb.getEmpireId();
        }
        mVotes = pb.getVotes();
        mDate = new DateTime(pb.getDate() * 1000, DateTimeZone.UTC);
    }

    public void toProtocolBuffer(Messages.AllianceRequestVote.Builder pb) {
        pb.setId(mID);
        pb.setAllianceId(mAllianceID);
        pb.setAllianceRequestId(mAllianceRequestID);
        pb.setEmpireId(mEmpireID);
        pb.setVotes(mVotes);
        pb.setDate(mDate.getMillis() / 1000);
    }
}
