package au.com.codeka.common.model;

import org.apache.commons.lang3.StringEscapeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class BaseChatMessage {
    protected int mID;
    protected String mMessage;
    protected String mEmpireKey;
    protected String mAllianceKey;
    protected DateTime mDatePosted;
    protected String mMessageEn;
    protected Integer mConversationID;
    protected MessageAction mAction;
    protected int mProfanityLevel;

    public static final int PROFANITY_NONE = 0;
    public static final int PROFANITY_MILD = 1;
    public static final int PROFANITY_STRONG = 2;

    public BaseChatMessage() {
        mDatePosted = new DateTime(DateTimeZone.UTC);
    }
    public BaseChatMessage(String message) {
        mMessage = message;
    }

    public int getID() {
        return mID;
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
    public Integer getEmpireID() {
        if (mEmpireKey == null) {
            return null;
        }
        return Integer.parseInt(mEmpireKey);
    }
    public void setEmpireID(int empireID) {
        mEmpireKey = Integer.toString(empireID);
    }
    public DateTime getDatePosted() {
        return mDatePosted;
    }
    public String getAllianceKey() {
        return mAllianceKey;
    }
    public void setAllianceKey(String allianceKey) {
        mAllianceKey = allianceKey;
    }
    public String getEnglishMessage() {
        return mMessageEn;
    }
    public Integer getConversationID() {
        return mConversationID;
    }
    public MessageAction getAction() {
        return mAction;
    }
    public int getProfanityLevel() {
        return mProfanityLevel;
    }

    public void fromProtocolBuffer(Messages.ChatMessage pb) {
        if (pb.hasId()) {
            mID = pb.getId();
        }
        mMessage = pb.getMessage();
        if (pb.getEmpireKey() != null && !pb.getEmpireKey().equals("")) {
            mEmpireKey = pb.getEmpireKey();
        }
        if (pb.getAllianceKey() != null && !pb.getAllianceKey().equals("")) {
            mAllianceKey = pb.getAllianceKey();
        }
        mDatePosted = new DateTime(pb.getDatePosted() * 1000, DateTimeZone.UTC);
        if (pb.hasMessageEn() && !pb.getMessageEn().equals("")) {
            mMessageEn = pb.getMessageEn();
        }
        if (pb.hasConversationId()) {
            mConversationID = pb.getConversationId();
        }
        if (pb.hasAction()) {
            mAction = MessageAction.fromNumber(pb.getAction().getNumber());
        } else {
            mAction = MessageAction.Normal;
        }
        if (pb.hasProfanityLevel()) {
            mProfanityLevel = pb.getProfanityLevel();
        }
    }

    public void toProtocolBuffer(Messages.ChatMessage.Builder pb, boolean encodeHtml) {
        pb.setId(mID);
        if (encodeHtml) {
            pb.setMessage(StringEscapeUtils.escapeHtml4(mMessage));
        } else {
            pb.setMessage(mMessage);
        }
        if (mEmpireKey != null) {
            pb.setEmpireKey(mEmpireKey);
        }
        if (mAllianceKey != null) {
            pb.setAllianceKey(mAllianceKey);
        }
        pb.setDatePosted(mDatePosted.getMillis() / 1000);
        if (mMessageEn != null && mMessageEn.length() > 0) {
            pb.setMessageEn(mMessageEn);
        }
        if (mConversationID != null) {
            pb.setConversationId(mConversationID);
        }
        if (mAction != null && mAction != MessageAction.Normal) {
            pb.setAction(Messages.ChatMessage.MessageAction.valueOf(mAction.getValue()));
        }
        pb.setProfanityLevel(mProfanityLevel);
    }

    public enum MessageAction {
        Normal(0),
        ParticipantAdded(1),
        ParticipantLeft(2);

        private int mValue;

        MessageAction(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static MessageAction fromNumber(int value) {
            for(MessageAction s : MessageAction.values()) {
                if (s.getValue() == value) {
                    return s;
                }
            }

            return MessageAction.Normal;
        }

    }
}
