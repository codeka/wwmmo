package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseChatMessage;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

public class ChatMessage extends BaseChatMessage {
    private Integer mEmpireID;
    private Integer mAllianceID;

    public ChatMessage() {
    }
    public ChatMessage(SqlResult res) throws SQLException {
        mID = res.getInt("id");
        mEmpireID = res.getInt("empire_id");
        if (mEmpireID != null) {
            mEmpireKey = Integer.toString(mEmpireID);
        }
        mAllianceID = res.getInt("alliance_id");
        if (mAllianceID != null) {
            mAllianceKey = Integer.toString(mAllianceID);
        }
        mDatePosted = res.getDateTime("posted_date");
        mMessage = res.getString("message");
        mMessageEn = res.getString("message_en");
        mConversationID = res.getInt("conversation_id");
        Integer action = res.getInt("action");
        if (action != null) {
            mAction = MessageAction.fromNumber(action);
        }
        mProfanityLevel = res.getInt("profanity_level");
    }
    public ChatMessage(int empireID, String message, MessageAction action, int conversationID) {
        mEmpireID = empireID;
        mEmpireKey = Integer.toString(empireID);
        mMessage = message;
        mAction = action;
        mConversationID = conversationID;
        mDatePosted = DateTime.now();
    }
    
    public static ChatMessage createServerMessage(String message) {
        ChatMessage msg = new ChatMessage();
        msg.mMessage = message;
        msg.mDatePosted = DateTime.now();
        msg.mAction = MessageAction.Normal;
        return msg;
    }

    public void setID(int id) {
        mID = id;
    }
    public int getAllianceID() {
        return mAllianceID;
    }
    public void setAction(MessageAction action) {
        mAction = action;
    }
    public void setDatePosted(DateTime dt) {
        mDatePosted = dt;
    }
    public void setEnglishMessage(String msg) {
        mMessageEn = msg;
    }
    public void setProfanityLevel(int profanityLevel) {
        mProfanityLevel = profanityLevel;
    }

    @Override
    public void fromProtocolBuffer(Messages.ChatMessage pb) {
        super.fromProtocolBuffer(pb);

        if (mEmpireKey != null) {
            mEmpireID = Integer.parseInt(mEmpireKey);
        }
        if (mAllianceKey != null) {
            mAllianceID = Integer.parseInt(mAllianceKey);
        }
    }
}
