package au.com.codeka.warworlds.intel;

import com.google.common.base.Throwables;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/** SimpleFormatter is supposed to take a format string, but I couldn't get it work. Doing our
 * own custom class lets us do some extra stuff, too. */
public class LogFormatter extends Formatter {
  private final DateTimeFormatter
      mDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

  public String format(LogRecord record) {
    StringBuilder sb = new StringBuilder();

    try {
      mDateTimeFormatter.printTo(sb, record.getMillis());
    } catch (IOException e) {
    }
    sb.append(" ");
    sb.append(record.getLevel().toString());
    sb.append(" ");

    if (record.getLoggerName().startsWith("au.com.codeka.warworlds.intel.")) {
      sb.append("accwi.");
      sb.append(record.getLoggerName().substring(31));
    } else if (record.getLoggerName().startsWith("au.com.codeka.")) {
      sb.append("acc.");
      sb.append(record.getLoggerName().substring(14));
    } else {
      sb.append(record.getLoggerName());
    }
    sb.append(String.format(" [%d] ", record.getThreadID()));
    sb.append(record.getMessage());
    if (record.getThrown() != null) {
      sb.append("\n");
      sb.append(Throwables.getStackTraceAsString(record.getThrown()));
    }

    sb.append("\n");
    return sb.toString();
  }
}
