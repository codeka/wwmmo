package au.com.codeka.warworlds.client.game.chat

import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.common.proto.ChatMessage
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Some helper methods for working with chat messages.
 */
object ChatHelper {
  private val CHAT_DATE_FORMAT = SimpleDateFormat("hh:mm a", Locale.US)
  private val URL_PATTERN = Pattern.compile(
      "((http|https)://|www\\.)([a-zA-Z0-9_-]{2,}\\.)+[a-zA-Z0-9_-]{2,}(/[a-zA-Z0-9/_.%#-]+)?")
  private val MARKDOWN_STRING = Pattern.compile(
      "(?<=(^|\\W))(\\*\\*|\\*|_|-)(.*?)\\1(?=($|\\W))")

  /**
   * Format the given [ChatMessage] as a simple, plain message with no extra formatting.
   *
   * @param msg The message to translate.
   * @param autoTranslate Whether to show the auto-translated or not.
   * @return An HTML-formatted message.
   */
  private fun formatPlain(msg: ChatMessage, autoTranslate: Boolean): String {
    var text = msg.message ?: return ""
    val messageEn = msg.message_en
    if (messageEn != null && autoTranslate) {
      text = messageEn
    }

//    int filterLevel = new GlobalOptions().chatProfanityFilterLevel().getValue();
//    if (mProfanityLevel > filterLevel) {
//      msg = "<b>--CENSORED--</b>";
//    }
    return linkify(markdown(text))
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
  fun format(
      msg: ChatMessage,
      isPublic: Boolean,
      messageOnly: Boolean,
      autoTranslate: Boolean): String {
    var text = formatPlain(msg, autoTranslate)
    if (msg.message_en != null && autoTranslate) {
      text = "<i>‹" + msg.message_en + "›</i>"
    }
    var isEnemy = false
    var isFriendly = false
    var isServer = false
    if (msg.empire_id != null) {
      if (msg.empire_id != EmpireManager.getMyEmpire().id) {
        isEnemy = true
      } else {
        isFriendly = true
      }
      if (!messageOnly) {
        val empire = EmpireManager.getEmpire(msg.empire_id)
        if (empire != null) {
          text = empire.display_name + " : " + text
        }
      }
    } else if (msg.date_posted != null) {
      isServer = true
      if (!messageOnly) {
        text = "[SERVER] : $text"
      }
    }
    if (msg.date_posted != null && !messageOnly) {
      text = CHAT_DATE_FORMAT.format(Date(msg.date_posted!!)) + " : " + text
    }
    if (isPublic) {
      if (isServer) {
        text = "<font color=\"#00ffff\"><b>$text</b></font>"
      } else if (msg.alliance_id != null) {
        text = "[Alliance] $text"
        text = if (isFriendly) {
          "<font color=\"#99ff99\">$text</font>"
        } else {
          "<font color=\"#9999ff\">$text</font>"
        }
      } else if (isEnemy) {
        text = "<font color=\"#ff9999\">$text</font>"
      } else if (isFriendly) {
        text = "<font color=\"#99ff99\">$text</font>"
      }
    } else { // !isPublic (e.g. alliance or private conversation)
      text = if (isFriendly) {
        "<font color=\"#99ff99\">$text</font>"
      } else {
        "<font color=\"#9999ff\">$text</font>"
      }
    }
    return text
  }

  /**
   * Converts some basic markdown formatting to HTML.
   */
  private fun markdown(markdown: String): String {
    val output = StringBuffer()
    val matcher = MARKDOWN_STRING.matcher(markdown)
    while (matcher.find()) {
      val kind = matcher.group(2)
      val text = matcher.group(3)
      when (kind) {
        "**" -> matcher.appendReplacement(output, String.format("<b>%s</b>", text))
        "*", "-", "_" -> matcher.appendReplacement(output, String.format("<i>%s</i>", text))
        else -> matcher.appendReplacement(output, text)
      }
    }
    matcher.appendTail(output)
    return output.toString()
  }

  /** Converts URLs to <a href> links. </a> */
  private fun linkify(line: String): String {
    val output = StringBuffer()
    val matcher = URL_PATTERN.matcher(line)
    while (matcher.find()) {
      var url = matcher.group(0)
      val display = url
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "http://$url"
      }
      matcher.appendReplacement(output, String.format("<a href=\"%s\">%s</a>", url, display))
    }
    matcher.appendTail(output)
    return output.toString()
  }
}