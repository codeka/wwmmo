package au.com.codeka.warworlds.server;

import com.google.common.base.Throwables;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * SimpleFormatter is supposed to take a format string, but I couldn't get it work. Doing our
 * own custom class lets us do some extra stuff, too.
 */
public class LogFormatter extends Formatter {
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

  public String format(LogRecord record) {
    StringBuilder sb = new StringBuilder();

    DATE_TIME_FORMATTER.printTo(sb, record.getMillis());
    sb.append(" ");
    sb.append(record.getLevel().toString());
    sb.append(" ");
    sb.append(record.getLoggerName());
    sb.append(String.format(Locale.US, " [%d] ", record.getThreadID()));
    try {
      if (record.getParameters() == null) {
        sb.append(record.getMessage());
      } else {
        sb.append(String.format(record.getMessage(), record.getParameters()));
      }
    } catch (Exception e) {
      sb.append("ERROR FORMATTING [");
      sb.append(record.getMessage());
      sb.append("]");
    }
    if (record.getThrown() != null) {
      sb.append("\n");
      sb.append(Throwables.getStackTraceAsString(record.getThrown()));
    }

    sb.append("\n");
    return sb.toString();
  }
}
