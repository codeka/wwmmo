package au.com.codeka.warworlds.model;

import jregex.MatchResult;
import jregex.Pattern;
import jregex.Replacer;
import jregex.Substitution;
import jregex.TextBuffer;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.text.Html;
import au.com.codeka.common.model.ChatMessage;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Model;

public class ChatMessageHelper {
    private static DateTimeFormatter sChatDateFormat;
    private static Pattern sUrlPattern;
    private static Pattern sMarkdownString;

    static {
        sChatDateFormat = DateTimeFormat.forPattern("hh:mm a");
        sUrlPattern = new Pattern(
                "((http|https)://|www\\.)([a-zA-Z0-9_-]{2,}\\.)+[a-zA-Z0-9_-]{2,}(/[a-zA-Z0-9/_.%#-]+)?");
        sMarkdownString = new Pattern(
                "(?<=(^|\\W))(\\*\\*|\\*|_|-)(.*?)\\1(?=($|\\W))");
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
    public static boolean shouldDisplay(ChatMessage msg, Location location) {
        if (location == Location.ALLIANCE_CHANNEL) {
            return (msg.alliance_key != null);
        } else {
            return true;
        }
    }

    /**
     * Formats this message for display in the mini chat view and/or
     * ChatActivity. It actually returns a snippet of formatted text, hence the
     * CharSequence.
     */
    public static CharSequence format(ChatMessage msg, Location location, boolean autoTranslate) {
        String msgValue = msg.message;
        boolean wasTranslated = false;
        if (msg.message_en != null && autoTranslate) {
            msgValue = msg.message_en;
            wasTranslated = true;
        }
        msgValue = linkify(markdown(msgValue));
        if (wasTranslated) {
            msgValue = "<i>‹"+msgValue+"›</i>";
        }

        boolean isEnemy = false;
        boolean isFriendly = false;
        boolean isServer = false;
        Empire empire = null;
        if (msg.empire_key != null) {
            empire = EmpireManager.i.getEmpire(msg.empire_key);
        }
        if (msg.empire_key != null && empire != null) {
            if (!msg.empire_key.equals(EmpireManager.i.getEmpire().key)) {
                isEnemy = true;
            } else {
                isFriendly = true;
            }
            msgValue = empire.display_name + " : " + msgValue;
        } else if (msg.empire_key == null && msg.date_posted == null) {
            isServer = true;
            msgValue = "[SERVER] : " + msgValue;
        }

        if (msg.date_posted != null) {
            msgValue = Model.toDateTime(msg.date_posted).withZone(
                    DateTimeZone.getDefault()).toString(sChatDateFormat) + " : " + msgValue;
        }

        if (location == Location.PUBLIC_CHANNEL) {
            if (isServer) {
                msgValue = "<font color=\"#00ffff\"><b>"+msgValue+"</b></font>";
            } else if (msg.alliance_key != null) {
                msgValue = "[Alliance] "+msgValue;
                msgValue = "<font color=\"#9999ff\">"+msgValue+"</font>";
            } else if (isEnemy) {
                msgValue = "<font color=\"#ff9999\">"+msgValue+"</font>";
            } else if (isFriendly) {
                msgValue = "<font color=\"#99ff99\">"+msgValue+"</font>";
            }
        } else if (location == Location.ALLIANCE_CHANNEL) {
            if (isFriendly) {
                msgValue = "<font color=\"#99ff99\">"+msgValue+"</font>";
            } else {
                msgValue = "<font color=\"#9999ff\">"+msgValue+"</font>";
            }
        } else {
        }

        return Html.fromHtml(msgValue);
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
}
