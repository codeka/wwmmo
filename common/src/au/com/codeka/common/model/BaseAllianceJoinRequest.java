package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class BaseAllianceJoinRequest {
    protected String mKey;
    protected String mAllianceKey;
    protected String mEmpireKey;
    protected String mMessage;
    protected DateTime mTimeRequested;
    protected RequestState mRequestState;

    public String getKey() {
        return mKey;
    }
    public String getAllianceKey() {
        return mAllianceKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public String getMessage() {
        return mMessage;
    }
    public DateTime getTimeRequested() {
        return mTimeRequested;
    }
    public RequestState getState() {
        return mRequestState;
    }
    public void setState(RequestState state) {
        mRequestState = state;
    }

    public void fromProtocolBuffer(Messages.AllianceJoinRequest pb) {
        mKey = pb.getKey();
        mAllianceKey = pb.getAllianceKey();
        mEmpireKey = pb.getEmpireKey();
        mMessage = pb.getMessage();
        mTimeRequested = new DateTime(pb.getTimeRequested() * 1000, DateTimeZone.UTC);
        mRequestState = RequestState.fromNumber(pb.getState().getNumber());
    }

    public void toProtocolBuffer(Messages.AllianceJoinRequest.Builder pb) {
        if (mKey != null) {
            pb.setKey(mKey);
        }
        pb.setAllianceKey(mAllianceKey);
        pb.setEmpireKey(mEmpireKey);
        pb.setMessage(mMessage);
        pb.setTimeRequested(mTimeRequested.getMillis() / 1000);
        pb.setState(Messages.AllianceJoinRequest.RequestState.valueOf(mRequestState.getValue()));
    }

    public enum RequestState
    {
        PENDING(0),
        ACCEPTED(1),
        REJECTED(2);

        private int mValue;

        RequestState(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static RequestState fromNumber(int value) {
            for(RequestState rs : RequestState.values()) {
                if (rs.getValue() == value) {
                    return rs;
                }
            }

            return RequestState.PENDING;
        }

    }
}
