package au.com.codeka.warworlds.server.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseChatMessage;

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
    }

    public int getID() {
        return mID;
    }
    public int getEmpireID() {
        return mEmpireID;
    }
    public int getAllianceID() {
        return mAllianceID;
    }
}
