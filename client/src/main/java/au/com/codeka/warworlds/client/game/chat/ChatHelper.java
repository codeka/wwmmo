package au.com.codeka.warworlds.client.game.chat;

import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.Empire;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some helper methods for working with chat messages.
 */
public class ChatHelper {
  private static final SimpleDateFormat CHAT_DATE_FORMAT =
      new SimpleDateFormat("hh:mm a", Locale.US);
  private static final Pattern URL_PATTERN = Pattern.compile(
      "((http|https)://|www\\.)([a-zA-Z0-9_-]{2,}\\.)+[a-zA-Z0-9_-]{2,}(/[a-zA-Z0-9/_.%#-]+)?");
  private static final Pattern MARKDOWN_STRING = Pattern.compile(
      "(?<=(^|\\W))(\\*\\*|\\*|_|-)(.*?)\\1(?=($|\\W))");

  /**
   * Format the given {@link ChatMessage} as a simple, plain message with no extra formatting.
   *
   * @param msg The message to translate.
   * @param autoTranslate Whether to show the auto-translated or not.
   * @return An HTML-formatted message.
   */
  public static String formatPlain(ChatMessage msg, boolean autoTranslate) {
    String text = msg.message;
    if (msg.message_en != null && autoTranslate) {
      text = msg.message_en;
    }

//    int filterLevel = new GlobalOptions().chatProfanityFilterLevel().getValue();
//    if (mProfanityLevel > filterLevel) {
//      msg = "<b>--CENSORED--</b>";
//    }

    return linkify(markdown(text));
  }

  /**
   * Format the given message.
   *
   * @param msg The message to format.
   * @param isPublic Whether the message is public or private.
   * @param messageOnly Whether to translate the message only, or include the post date as well.
   * @param autoTranslate Whether to include the auto-translated message instead (if there is one).
   * @return An HTML-formatted version of the message.
   */
  public static String format(
      ChatMessage msg,
      boolean isPublic,
      boolean messageOnly,
      boolean autoTranslate) {
    String text = formatPlain(msg, autoTranslate);
    if (msg.message_en != null && autoTranslate) {
      text = "<i>‹" + msg.message_en + "›</i>";
    }

    boolean isEnemy = false;
    boolean isFriendly = false;
    boolean isServer = false;
    if (msg.empire_id != null) {
      if (!msg.empire_id.equals(EmpireManager.i.getMyEmpire().id)) {
        isEnemy = true;
      } else {
        isFriendly = true;
      }
      if (!messageOnly) {
        Empire empire = EmpireManager.i.getEmpire(msg.empire_id);
        if (empire != null) {
          text = empire.display_name + " : " + text;
        }
      }
    } else if (msg.date_posted != null) {
      isServer = true;
      if (!messageOnly) {
        text = "[SERVER] : " + text;
      }
    }

    if (msg.date_posted != null && !messageOnly) {
      text = CHAT_DATE_FORMAT.format(new Date(msg.date_posted)) + " : " + text;
    }

    if (isPublic) {
      if (isServer) {
        text = "<font color=\"#00ffff\"><b>" + text + "</b></font>";
      } else if (msg.alliance_id != null) {
        text = "[Alliance] " + text;
        if (isFriendly) {
          text = "<font color=\"#99ff99\">" + text + "</font>";
        } else {
          text = "<font color=\"#9999ff\">" + text + "</font>";
        }
      } else if (isEnemy) {
        text = "<font color=\"#ff9999\">" + text + "</font>";
      } else if (isFriendly) {
        text = "<font color=\"#99ff99\">" + text + "</font>";
      }
    } else { // !isPublic (e.g. alliance or private conversation)
      if (isFriendly) {
        text = "<font color=\"#99ff99\">" + text + "</font>";
      } else {
        text = "<font color=\"#9999ff\">" + text + "</font>";
      }
    }

    return text;
  }

  /**
   * Converts some basic markdown formatting to HTML.
   */
  private static String markdown(String markdown) {
    StringBuffer output = new StringBuffer();
    Matcher matcher = MARKDOWN_STRING.matcher(markdown);
    while (matcher.find()) {
      String kind = matcher.group(2);
      String text = matcher.group(3);
      switch (kind) {
        case "**":
          matcher.appendReplacement(output, String.format("<b>%s</b>", text));
          break;
        case "*":
        case "-":
        case "_":
          matcher.appendReplacement(output, String.format("<i>%s</i>", text));
          break;
        default:
          matcher.appendReplacement(output, text);
          break;
      }
    }
    matcher.appendTail(output);

    return output.toString();
  }

  /** Converts URLs to <a href> links. */
  private static String linkify(String line) {
    StringBuffer output = new StringBuffer();
    Matcher matcher = URL_PATTERN.matcher(line);
    while (matcher.find()) {
      String url = matcher.group(0);
      String display = url;
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "http://" + url;
      }

      matcher.appendReplacement(output, String.format("<a href=\"%s\">%s</a>", url, display));
    }
    matcher.appendTail(output);

    return output.toString();
  }
}
