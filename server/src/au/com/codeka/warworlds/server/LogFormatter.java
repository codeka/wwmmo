package au.com.codeka.warworlds.server;

import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Throwables;

import javax.annotation.Nullable;

/**
 * SimpleFormatter is supposed to take a format string, but I couldn't get it work. Doing our
 * own custom class lets us do some extra stuff, too.
 */
public class LogFormatter extends Formatter {
  private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(
      "yyyy-MM-dd HH:mm:ss");

  @Nullable
  private final Pattern tagFilterPattern;

  public LogFormatter() {
    System.out.println(getClass().getName() + ".filter");
    String filter =
        LogManager.getLogManager().getProperty(getClass().getName() + ".filter");
    System.out.println("filter: " + filter);
    if (filter != null && !filter.isEmpty()) {
      tagFilterPattern = Pattern.compile(filter);
    } else {
      tagFilterPattern = null;
    }
  }

  public String format(LogRecord record) {
    if (tagFilterPattern != null && !tagFilterPattern.matcher(record.getLoggerName()).matches()) {
      return null;
    }

    StringBuilder sb = new StringBuilder();

    DATE_TIME_FORMATTER.printTo(sb, record.getMillis());
    sb.append(" ");
    sb.append(record.getLevel().toString());
    sb.append(" ");

    if (record.getLoggerName().startsWith("au.com.codeka.warworlds.server.")) {
      sb.append("accws.");
      sb.append(record.getLoggerName().substring(31));
    } else if (record.getLoggerName().startsWith("au.com.codeka.")) {
      sb.append("acc.");
      sb.append(record.getLoggerName().substring(14));
    } else {
      sb.append(record.getLoggerName());
    }
    sb.append(String.format(Locale.US, " [%d] ", record.getThreadID()));
    try {
      sb.append(String.format(record.getMessage(), record.getParameters()));
    } catch (Exception e) {
      sb.append("ERROR FORMATTING ");
      sb.append(e.getMessage());
      sb.append(" [");
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
