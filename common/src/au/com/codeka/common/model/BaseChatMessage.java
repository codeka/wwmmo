package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class BaseChatMessage {
    protected String mMessage;
    protected String mEmpireKey;
    protected String mAllianceKey;
    protected BaseEmpire mEmpire;
    protected DateTime mDatePosted;
    protected String mDetectedLanguage;

    public BaseChatMessage() {
        mDatePosted = new DateTime(DateTimeZone.UTC);
    }
    public BaseChatMessage(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
    public void setMessage(String msg) {
        mMessage = msg;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public BaseEmpire getEmpire() {
        return mEmpire;
    }
    public void setEmpire(BaseEmpire emp) {
        mEmpire = emp;
        if (emp != null) {
            mEmpireKey = emp.getKey();
        }
    }
    public void setAllianceChat(boolean isAllianceChat) {
        if (isAllianceChat && mEmpire != null && mEmpire.getAlliance() != null) {
            mAllianceKey = mEmpire.getAlliance().getKey();
        } else {
            mAllianceKey = null;
        }
    }
    public DateTime getDatePosted() {
        return mDatePosted;
    }
    public String getAllianceKey() {
        return mAllianceKey;
    }
    public String getDetectedLanguage() {
        return mDetectedLanguage;
    }
    public void setDetectedLanguage(String langCode) {
        mDetectedLanguage = langCode;
    }

    /**
     * We format messages slightly differently depending on whether it's an
     * alliance chat, private message, public message and where it's actually being
     * displayed. This enum is used to describe which channel we're displaying.
     */
    public enum Location {
        PUBLIC_CHANNEL(0),
        ALLIANCE_CHANNEL(1);

        private int mNumber;

        Location(int number) {
            mNumber = number;
        }

        public int getNumber() {
            return mNumber;
        }

        public static Location fromNumber(int number) {
            return Location.values()[number];
        }
    }

    /**
     * Determines whether this chat message should be visible in the given location.
     */
    public boolean shouldDisplay(Location location) {
        if (location == Location.ALLIANCE_CHANNEL) {
            return (mAllianceKey != null);
        } else {
            return true;
        }
    }

    public void fromProtocolBuffer(Messages.ChatMessage pb) {
        mMessage = pb.getMessage();
        if (pb.getEmpireKey() != null && !pb.getEmpireKey().equals("")) {
            mEmpireKey = pb.getEmpireKey();
        }
        if (pb.getAllianceKey() != null && !pb.getAllianceKey().equals("")) {
            mAllianceKey = pb.getAllianceKey();
        }
        mDatePosted = new DateTime(pb.getDatePosted() * 1000, DateTimeZone.UTC);
    }

    public void toProtocolBuffer(Messages.ChatMessage.Builder pb) {
        pb.setMessage(mMessage);
        if (mEmpireKey != null) {
            pb.setEmpireKey(mEmpireKey);
        }
        if (mAllianceKey != null) {
            pb.setAllianceKey(mAllianceKey);
        }
        pb.setDatePosted(mDatePosted.getMillis() / 1000);
    }
}
