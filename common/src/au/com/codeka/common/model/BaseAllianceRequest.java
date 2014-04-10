package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.protobuf.ByteString;

import au.com.codeka.common.protobuf.Messages;

public abstract class BaseAllianceRequest {
    protected int mID;
    protected int mAllianceID;
    protected int mRequestEmpireID;
    protected DateTime mRequestDate;
    protected RequestType mRequestType;
    protected String mMessage;
    protected RequestState mState;
    protected int mNumVotes;
    protected Integer mTargetEmpireID;
    protected Float mAmount;
    protected byte[] mPngImage;
    protected String mNewName;
    protected List<BaseAllianceRequestVote> mVotes;

    public int getID() {
        return mID;
    }
    public int getAllianceID() {
        return mAllianceID;
    }
    public int getRequestEmpireID() {
        return mRequestEmpireID;
    }
    public DateTime getRequestDate() {
        return mRequestDate;
    }
    public RequestType getRequestType() {
        return mRequestType;
    }
    public String getMessage() {
        return mMessage;
    }
    public RequestState getState() {
        return mState;
    }
    public int getNumVotes() {
        return mNumVotes;
    }
    public Integer getTargetEmpireID() {
        return mTargetEmpireID;
    }
    public Float getAmount() {
        return mAmount;
    }
    public byte[] getPngImage() {
        return mPngImage;
    }
    public String getNewName() {
        return mNewName;
    }
    public List<BaseAllianceRequestVote> getVotes() {
        if (mVotes == null) {
            mVotes = new ArrayList<BaseAllianceRequestVote>();
        }
        return mVotes;
    }

    protected abstract BaseAllianceRequestVote createVote(Messages.AllianceRequestVote pb);

    public void fromProtocolBuffer(Messages.AllianceRequest pb) {
        if (pb.hasId()) {
            mID = pb.getId();
        }
        mAllianceID = pb.getAllianceId();
        mRequestEmpireID = pb.getRequestEmpireId();
        mRequestDate = new DateTime(pb.getRequestDate() * 1000, DateTimeZone.UTC);
        mRequestType = RequestType.fromNumber(pb.getRequestType().getNumber());
        mMessage = pb.getMessage();
        mState = RequestState.fromNumber(pb.getState().getNumber());
        mNumVotes = pb.getNumVotes();
        if (pb.hasTargetEmpireId()) {
            mTargetEmpireID = pb.getTargetEmpireId();
        }
        if (pb.hasAmount()) {
            mAmount = pb.getAmount();
        }
        if (pb.hasPngImage()) {
            mPngImage = pb.getPngImage().toByteArray();
        }
        if (pb.hasNewName()) {
            mNewName = pb.getNewName();
        }
        if (pb.getVoteList() != null) {
            mVotes = new ArrayList<BaseAllianceRequestVote>();
            for (Messages.AllianceRequestVote vote_pb : pb.getVoteList()) {
                mVotes.add(createVote(vote_pb));
            }
        }
    }

    public void toProtocolBuffer(Messages.AllianceRequest.Builder pb) {
        pb.setId(mID);
        pb.setAllianceId(mAllianceID);
        pb.setRequestEmpireId(mRequestEmpireID);
        pb.setRequestDate(mRequestDate.getMillis() / 1000);
        pb.setRequestType(Messages.AllianceRequest.RequestType.valueOf(mRequestType.getNumber()));
        pb.setMessage(mMessage);
        pb.setState(Messages.AllianceRequest.RequestState.valueOf(mState.getNumber()));
        pb.setNumVotes(mNumVotes);
        if (mTargetEmpireID != null) {
            pb.setTargetEmpireId((int) mTargetEmpireID);
        }
        if (mAmount != null) {
            pb.setAmount((float) mAmount);
        }
        if (mPngImage != null) {
            pb.setPngImage(ByteString.copyFrom(mPngImage));
        }
        if (mNewName != null) {
            pb.setNewName(mNewName);
        }
        if (mVotes != null) {
            for (BaseAllianceRequestVote vote : mVotes) {
                Messages.AllianceRequestVote.Builder vote_pb = Messages.AllianceRequestVote.newBuilder();
                vote.toProtocolBuffer(vote_pb);
                pb.addVote(vote_pb);
            }
        }
    }

    public enum RequestType {
        JOIN(0, 5),
        LEAVE(1, 0),
        KICK(2, 10),
        DEPOSIT_CASH(3, 0),
        WITHDRAW_CASH(4, 10),
        CHANGE_IMAGE(5, 10),
        CHANGE_NAME(6, 10);

        private int mNumber;
        private int mRequiredVotes;

        RequestType(int number, int requiredVotes) {
            mNumber = number;
            mRequiredVotes = requiredVotes;
        }

        public int getNumber() {
            return mNumber;
        }

        public int getRequiredVotes() {
            return mRequiredVotes;
        }

        public static RequestType fromNumber(int number) {
            RequestType[] values = RequestType.values();
            return values[number];
        }
    }

    public enum RequestState {
        PENDING(0),
        ACCEPTED(1),
        REJECTED(2),
        WITHDRAWN(3);

        private int mNumber;

        RequestState(int number) {
            mNumber = number;
        }

        public int getNumber() {
            return mNumber;
        }

        public static RequestState fromNumber(int number) {
            return RequestState.values()[number];
        }
    }
}
