package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.text.Html;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class ChatMessage {
    private String mMessage;
    private String mEmpireKey;
    private Empire mEmpire;
    private DateTime mDatePosted;

    private static DateTimeFormatter sChatDateFormat;

    public ChatMessage() {
    }
    public ChatMessage(String message) {
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
    public Empire getEmpire() {
        return mEmpire;
    }
    public void setEmpire(Empire emp) {
        mEmpire = emp;
    }
    public DateTime getDatePosted() {
        return mDatePosted;
    }

    /**
     * Formats this message for display in the mini chat view and/or
     * ChatActivity. It actually returns a snippet of formatted text, hence the
     * CharSequence.
     */
    public CharSequence format() {
        String msg = mMessage;

        boolean isEnemy = false;
        boolean isFriendly = false;
        boolean isServer = false;
        if (mEmpireKey != null && mEmpire != null) {
            if (!mEmpireKey.equals(EmpireManager.getInstance().getEmpire().getKey())) {
                isEnemy = true;
            } else {
                isFriendly = true;
            }
            msg = mEmpire.getDisplayName() + " : " + msg;
        } else if (mEmpireKey == null && mDatePosted != null) {
            isServer = true;
            msg = "[SERVER] : " + msg;
        }

        if (mDatePosted != null) {
            if (sChatDateFormat == null) {
                sChatDateFormat = DateTimeFormat.forPattern("hh:mm a");
            }
            msg = mDatePosted.toString(sChatDateFormat) + " : " + msg;
        }

        if (isServer) {
            msg = "<font color=\"#00ffff\"><b>"+msg+"</b></font>";
        } else if (isEnemy) {
            msg = "<font color=\"#ff9999\">"+msg+"</font>";
        } else if (isFriendly) {
            msg = "<font color=\"#99ff99\">"+msg+"</font>";
        }

        return Html.fromHtml(msg);
    }

    public static ChatMessage fromProtocolBuffer(Messages.ChatMessage pb) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.mMessage = pb.getMessage();
        if (pb.getEmpireKey() != null && !pb.getEmpireKey().isEmpty()) {
            chatMessage.mEmpireKey = pb.getEmpireKey();
        }
        chatMessage.mDatePosted = new DateTime(pb.getDatePosted() * 1000, DateTimeZone.UTC);
        return chatMessage;
    }
}
