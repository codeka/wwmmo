package au.com.codeka.warworlds.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import au.com.codeka.common.model.BaseChatMessage;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.GlobalOptions;

public class ChatMessage extends BaseChatMessage {
    private static DateTimeFormatter sChatDateFormat;
    private static Pattern sUrlPattern;
    private static Pattern sMarkdownString;

    {
        sChatDateFormat = DateTimeFormat.forPattern("hh:mm a");
        sUrlPattern = Pattern.compile(
                "((http|https)://|www\\.)([a-zA-Z0-9_-]{2,}\\.)+[a-zA-Z0-9_-]{2,}(/[a-zA-Z0-9/_.%#-]+)?");
        sMarkdownString = Pattern.compile(
                "(?<=(^|\\W))(\\*\\*|\\*|_|-)(.*?)\\1(?=($|\\W))");
    }

    public ChatMessage() {
        super();
    }
    public ChatMessage(String message) {
        super(message);
    }

    public void setConversation(ChatConversation conversation) {
        if (conversation.getID() == 0) {
            mConversationID = null;
        } else if (conversation.getID() < 0 && EmpireManager.i.getEmpire().getAlliance() != null) {
            mConversationID = null;
            mAllianceKey = EmpireManager.i.getEmpire().getAlliance().getKey();
        } else {
            mConversationID = conversation.getID();
        }
    }

    public String formatPlain(boolean autoTranslate) {
        String msg = mMessage;
        if (mMessageEn != null && autoTranslate) {
            msg = mMessageEn;
        }

        int filterLevel = new GlobalOptions().chatProfanityFilterLevel().getValue();
        if (mProfanityLevel > filterLevel) {
            msg = "<b>--CENSORED--</b>";
        }

        return linkify(markdown(msg));
    }

    /**
     * Formats this message for display in the mini chat view and/or
     * ChatActivity. It actually returns a snippet of formatted text, hence the
     * CharSequence.
     */
    public String format(boolean isPublic, boolean messageOnly, boolean autoTranslate) {
        String msg = formatPlain(autoTranslate);
        if (mMessageEn != null && autoTranslate) {
            msg = "<i>‹"+msg+"›</i>";
        }

        boolean isEnemy = false;
        boolean isFriendly = false;
        boolean isServer = false;
        if (mEmpireKey != null && mEmpire != null) {
            if (!mEmpireKey.equals(EmpireManager.i.getEmpire().getKey())) {
                isEnemy = true;
            } else {
                isFriendly = true;
            }
            if (!messageOnly) {
                msg = mEmpire.getDisplayName() + " : " + msg;
            }
        } else if (mEmpireKey == null && mDatePosted != null) {
            isServer = true;
            if (!messageOnly) {
                msg = "[SERVER] : " + msg;
            }
        }

        if (mDatePosted != null && !messageOnly) {
            msg = mDatePosted.withZone(DateTimeZone.getDefault()).toString(sChatDateFormat) + " : " + msg;
        }

        if (isPublic) {
            if (isServer) {
                msg = "<font color=\"#00ffff\"><b>"+msg+"</b></font>";
            } else if (mAllianceKey != null) {
                msg = "[Alliance] "+msg;
                if (isFriendly) {
                    msg = "<font color=\"#99ff99\">"+msg+"</font>";
                } else {
                    msg = "<font color=\"#9999ff\">"+msg+"</font>";
                }
            } else if (isEnemy) {
                msg = "<font color=\"#ff9999\">"+msg+"</font>";
            } else if (isFriendly) {
                msg = "<font color=\"#99ff99\">"+msg+"</font>";
            }
        } else { // !isPublic (e.g. alliance or private conversation)
            if (isFriendly) {
                msg = "<font color=\"#99ff99\">"+msg+"</font>";
            } else {
                msg = "<font color=\"#9999ff\">"+msg+"</font>";
            }
        }

        return msg;
    }

    /**
     * Converts some basic markdown formatting to HTML.
     */
    private static String markdown(String markdown) {
        StringBuffer output = new StringBuffer();
        Matcher matcher = sMarkdownString.matcher(markdown);
        while (matcher.find()) {
            String kind = matcher.group(2);
            String text = matcher.group(3);
            if (kind.equals("**")) {
                matcher.appendReplacement(output, String.format("<b>%s</b>", text));
            } else if (kind.equals("*") || kind.equals("-") || kind.equals("_")) {
                matcher.appendReplacement(output, String.format("<i>%s</i>", text));
            } else {
                matcher.appendReplacement(output, text);
            }
        }
        matcher.appendTail(output);

        return output.toString();
    }

    /**
     * Converts URLs to <a href> links.
     */
    private static String linkify(String line) {
        StringBuffer output = new StringBuffer();
        Matcher matcher = sUrlPattern.matcher(line);
        while (matcher.find()) {
            String url = matcher.group(0);
            String display = url;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://"+url;
            }

            matcher.appendReplacement(output, String.format("<a href=\"%s\">%s</a>",
                    url, display));
        }
        matcher.appendTail(output);

        return output.toString();
    }

    @Override
    public void toProtocolBuffer(Messages.ChatMessage.Builder pb, boolean encodeHtml) {
        super.toProtocolBuffer(pb, encodeHtml);
    }

    public Messages.ChatMessage toProtocolBuffer() {
        Messages.ChatMessage.Builder pb = Messages.ChatMessage.newBuilder();
        toProtocolBuffer(pb, false);
        return pb.build();
    }
}
