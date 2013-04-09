package au.com.codeka.warworlds.model;

import jregex.MatchResult;
import jregex.Pattern;
import jregex.Replacer;
import jregex.Substitution;
import jregex.TextBuffer;

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
    private String mDetectedLanguage;

    private static DateTimeFormatter sChatDateFormat;
    private static Pattern sUrlPattern;
    private static Pattern sMarkdownString;

    {
        sChatDateFormat = DateTimeFormat.forPattern("hh:mm a");
        sUrlPattern = new Pattern(
                "((http|https)://|www\\.)([a-zA-Z0-9_-]{2,}\\.)+[a-zA-Z0-9_-]{2,}(/[a-zA-Z0-9/_.%#-]+)?");
        sMarkdownString = new Pattern(
                "(?<=(^|\\W))(\\*\\*|\\*|_|-)(.*?)\\1(?=($|\\W))");
    }

    public ChatMessage() {
        mDatePosted = new DateTime(DateTimeZone.UTC);
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
        if (emp != null) {
            mEmpireKey = emp.getKey();
        }
    }
    public DateTime getDatePosted() {
        return mDatePosted;
    }
    public String getDetectedLanguage() {
        return mDetectedLanguage;
    }
    public void setDetectedLanguage(String langCode) {
        mDetectedLanguage = langCode;
    }

    /**
     * Formats this message for display in the mini chat view and/or
     * ChatActivity. It actually returns a snippet of formatted text, hence the
     * CharSequence.
     */
    public CharSequence format() {
        String msg = mMessage;
        msg = linkify(markdown(msg));

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
            msg = mDatePosted.withZone(DateTimeZone.getDefault()).toString(sChatDateFormat) + " : " + msg;
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

    /**
     * Converts some basic markdown formatting to HTML.
     */
    private static String markdown(String markdown) {
        Replacer replacer = sMarkdownString.replacer(new Substitution() {
            @Override
            public void appendSubstitution(MatchResult match, TextBuffer dest) {
                String kind = match.group(2);
                String text = match.group(3);
                if (kind.equals("**")) {
                    dest.append("<b>");
                    dest.append(text);
                    dest.append("</b>");
                } else if (kind.equals("*") || kind.equals("-") || kind.equals("_")) {
                    dest.append("<i>");
                    dest.append(text);
                    dest.append("</i>");
                } else {
                    dest.append(text);
                }
            }
        });

        return replacer.replace(markdown);
    }

    /**
     * Converts URLs to <a href> links.
     */
    private static String linkify(String line) {
        Replacer replacer = sUrlPattern.replacer(new Substitution() {
            @Override
            public void appendSubstitution(MatchResult match, TextBuffer dest) {
                String url = match.group(0);
                String display = url;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://"+url;
                }
                dest.append("<a href=\"");
                dest.append(url);
                dest.append("\">");
                dest.append(display);
                dest.append("</a>");
            }
        });

        return replacer.replace(line);
    }

    public static ChatMessage fromProtocolBuffer(Messages.ChatMessage pb) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.mMessage = pb.getMessage();
        if (pb.getEmpireKey() != null && !pb.getEmpireKey().equals("")) {
            chatMessage.mEmpireKey = pb.getEmpireKey();
        }
        chatMessage.mDatePosted = new DateTime(pb.getDatePosted() * 1000, DateTimeZone.UTC);
        return chatMessage;
    }
}
