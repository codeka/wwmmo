package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseChatMessage;
import au.com.codeka.common.protobuf.Messages;

public class ChatMessage extends BaseChatMessage {
    private int mEmpireID;
    private int mAllianceID;

    public ChatMessage() {
    }
    public ChatMessage(ResultSet rs) throws SQLException {
        mID = rs.getInt("id");
        mEmpireID = rs.getInt("empire_id");
        if (!rs.wasNull()) {
            mEmpireKey = Integer.toString(mEmpireID);
        }
        mAllianceID = rs.getInt("alliance_id");
        if (!rs.wasNull()) {
            mAllianceKey = Integer.toString(mAllianceID);
        }
        mDatePosted = new DateTime(rs.getTimestamp("posted_date").getTime());
        mMessage = rs.getString("message");
        mMessageEn = rs.getString("message_en");
        mConversationID = rs.getInt("conversation_id");
        if (rs.wasNull()) {
            mConversationID = null;
        }
        int action = rs.getInt("action");
        if (!rs.wasNull()) {
            mAction = MessageAction.fromNumber(action);
        }
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
    public int getEmpireID() {
        return mEmpireID;
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
