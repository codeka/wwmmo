package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.AllianceRequest;
import au.com.codeka.common.protobuf.AllianceRequestVote;
import okio.ByteString;

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
            mVotes = new ArrayList<>();
        }
        return mVotes;
    }

    protected abstract BaseAllianceRequestVote createVote(AllianceRequestVote pb);

    public void fromProtocolBuffer(AllianceRequest pb) {
        if (pb.id != null) {
            mID = pb.id;
        }
        mAllianceID = pb.alliance_id;
        mRequestEmpireID = pb.request_empire_id;
        mRequestDate = new DateTime(pb.request_date * 1000, DateTimeZone.UTC);
        mRequestType = RequestType.fromNumber(pb.request_type.getValue());
        mMessage = pb.message;
        mState = RequestState.fromNumber(pb.state.getValue());
        mNumVotes = pb.num_votes;
        mTargetEmpireID = pb.target_empire_id;
        mAmount = pb.amount;
        if (pb.png_image != null) {
            mPngImage = pb.png_image.toByteArray();
        }
        mNewName = pb.new_name;
        if (pb.vote.size() > 0) {
            mVotes = new ArrayList<>();
            for (AllianceRequestVote vote_pb : pb.vote) {
                mVotes.add(createVote(vote_pb));
            }
        }
    }

    public void toProtocolBuffer(AllianceRequest.Builder pb) {
        pb.id = mID;
        pb.alliance_id = mAllianceID;
        pb.request_empire_id = mRequestEmpireID;
        pb.request_date = mRequestDate.getMillis() / 1000;
        pb.request_type = (AllianceRequest.RequestType.valueOf(mRequestType.toString()));
        pb.message = mMessage;
        pb.state = AllianceRequest.RequestState.valueOf(mState.toString());
        pb.num_votes = mNumVotes;
        pb.target_empire_id = mTargetEmpireID;
        pb.amount = mAmount;
        if (mPngImage != null) {
            pb.png_image = ByteString.of(mPngImage);
        }
        pb.new_name = mNewName;
        if (mVotes != null) {
            pb.vote = new ArrayList<>();
            for (BaseAllianceRequestVote vote : mVotes) {
                AllianceRequestVote.Builder vote_pb = new AllianceRequestVote.Builder();
                vote.toProtocolBuffer(vote_pb);
                pb.vote.add(vote_pb.build());
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
