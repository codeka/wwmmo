package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.warworlds.model.protobuf.Messages;

public class AllianceJoinRequest {
    private String mKey;
    private String mAllianceKey;
    private String mEmpireKey;
    private String mMessage;
    private DateTime mTimeRequested;
    private RequestState mRequestState;

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

    public static AllianceJoinRequest fromProtocolBuffer(Messages.AllianceJoinRequest pb) {
        AllianceJoinRequest ajr = new AllianceJoinRequest();
        ajr.mKey = pb.getKey();
        ajr.mAllianceKey = pb.getAllianceKey();
        ajr.mEmpireKey = pb.getEmpireKey();
        ajr.mMessage = pb.getMessage();
        ajr.mTimeRequested = new DateTime(pb.getTimeRequested() * 1000, DateTimeZone.UTC);
        ajr.mRequestState = RequestState.fromNumber(pb.getState().getNumber());
        return ajr;
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
