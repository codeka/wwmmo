package au.com.codeka.warworlds.server

import com.google.common.base.Throwables
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Formatter
import java.util.logging.LogRecord

/**
 * SimpleFormatter is supposed to take a format string, but I couldn't get it work. Doing our
 * own custom class lets us do some extra stuff, too.
 */
class LogFormatter : Formatter() {
  companion object {
    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
  }

  override fun format(record: LogRecord): String {
    val sb = StringBuilder()
    DATE_TIME_FORMATTER.printTo(sb, record.millis)
    sb.append(" ")
    sb.append(record.level.toString())
    sb.append(" ")
    sb.append(record.loggerName)
    sb.append(String.format(Locale.US, " [%d] ", record.threadID))
    try {
      if (record.parameters == null) {
        sb.append(record.message)
      } else {
        sb.append(String.format(record.message, *record.parameters))
      }
    } catch (e: Exception) {
      sb.append("ERROR FORMATTING [")
      sb.append(record.message)
      sb.append("]")
    }
    if (record.thrown != null) {
      sb.append("\n")
      sb.append(Throwables.getStackTraceAsString(record.thrown))
    }
    sb.append("\n")
    return sb.toString()
  }
}
