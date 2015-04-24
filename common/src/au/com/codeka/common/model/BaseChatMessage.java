package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;

import au.com.codeka.common.protobuf.ChatMessage;

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

    public void fromProtocolBuffer(ChatMessage pb) {
        mID = pb.id;
        mMessage = pb.message;
        mEmpireKey = Strings.emptyToNull(pb.empire_key);
        mAllianceKey = Strings.emptyToNull(pb.alliance_key);
        mDatePosted = new DateTime(pb.date_posted * 1000, DateTimeZone.UTC);
        mMessageEn = Strings.emptyToNull(pb.message_en);
        mConversationID = pb.conversation_id;
        mAction = pb.action == null ? MessageAction.Normal : MessageAction.fromNumber(pb.action.getValue());
        mProfanityLevel = pb.profanity_level == null ? 0 : pb.profanity_level;
    }

    public void toProtocolBuffer(ChatMessage pb, boolean encodeHtml) {
        pb.id = mID;
        if (encodeHtml) {
            pb.message = HtmlEscapers.htmlEscaper().escape(mMessage);
        } else {
            pb.message = mMessage;
        }
        pb.empire_key = mEmpireKey;
        pb.alliance_key = mAllianceKey;
        pb.date_posted = mDatePosted.getMillis() / 1000;
        pb.message_en = Strings.emptyToNull(mMessageEn);
        pb.conversation_id = mConversationID;
        if (mAction != null && mAction != MessageAction.Normal) {
            pb.action = ChatMessage.MessageAction.valueOf(mAction.toString());
        }
        pb.profanity_level = mProfanityLevel;
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
