package au.com.codeka.warworlds.server;

import java.io.IOException;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/** SimpleFormatter is supposed to take a format string, but I couldn't get it work. Doing our
 * own custom class lets us do some extra stuff, too. */
public class LogFormatter extends Formatter {
    private final DateTimeFormatter mDateTimeFormatter = DateTimeFormat.forPattern(
            "yyyy-MM-dd HH:mm:ss");

    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        try {
            mDateTimeFormatter.printTo(sb, record.getMillis());
        } catch (IOException e) {
        }
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
        sb.append(String.format(" [%d] ", record.getThreadID()));
        try {
            sb.append(String.format(record.getMessage(), record.getParameters()));
        } catch (Exception e) {
            sb.append("ERROR FORMATTING [" + record.getMessage() + "]");
        }
        if (record.getThrown() != null) {
            sb.append("\n");
            sb.append(ExceptionUtils.getStackTrace(record.getThrown()));
        }

        sb.append("\n");
        return sb.toString();
    }
}
