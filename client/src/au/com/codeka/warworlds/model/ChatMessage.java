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
import au.com.codeka.common.model.BaseChatMessage;
import au.com.codeka.common.protobuf.Messages;

public class ChatMessage extends BaseChatMessage {
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
        super();
    }
    public ChatMessage(String message) {
        super(message);
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

    /**
     * Formats this message for display in the mini chat view and/or
     * ChatActivity. It actually returns a snippet of formatted text, hence the
     * CharSequence.
     */
    public CharSequence format(Location location, boolean messageOnly, boolean autoTranslate) {
        String msg = mMessage;
        boolean wasTranslated = false;
        if (mMessageEn != null && autoTranslate) {
            msg = mMessageEn;
            wasTranslated = true;
        }
        msg = linkify(markdown(msg));
        if (wasTranslated) {
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

        if (location == Location.PUBLIC_CHANNEL) {
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
        } else if (location == Location.ALLIANCE_CHANNEL) {
            if (isFriendly) {
                msg = "<font color=\"#99ff99\">"+msg+"</font>";
            } else {
                msg = "<font color=\"#9999ff\">"+msg+"</font>";
            }
        } else {
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
