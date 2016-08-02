package au.com.codeka.warworlds.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * This is a helper class for working with logging. We support both Android Log.x() and Java's
 * built-in logger (for server-side).
 */
public class Log {
  public static final int ERROR = 0;
  public static final int WARNING = 1;
  public static final int INFO = 2;
  public static final int DEBUG = 3;

  private static LogImpl impl;
  public static void setImpl(LogImpl impl) {
    Log.impl = impl;
  }

  private String tag;
  @Nullable private String prefix;

  public Log(String tag) {
    this.tag = tag;
  }

  public void setPrefix(@Nullable String prefix) {
    this.prefix = prefix;
  }

  public void error(String fmt, Object... args) {
    if (impl == null || !impl.isLoggable(tag, ERROR)) {
      return;
    }
    impl.write(tag, ERROR, formatMsg(fmt, args));
  }

  public void warning(String fmt, Object... args) {
    if (impl == null || !impl.isLoggable(tag, WARNING)) {
      return;
    }
    impl.write(tag, WARNING, formatMsg(fmt, args));
  }

  public void info(String fmt, Object... args) {
    if (impl == null || !impl.isLoggable(tag, INFO)) {
      return;
    }
    impl.write(tag, INFO, formatMsg(fmt, args));
  }

  public void debug(String fmt, Object... args) {
    if (impl == null || !impl.isLoggable(tag, DEBUG)) {
      return;
    }
    impl.write(tag, DEBUG, formatMsg(fmt, args));
  }

  public boolean isDebugEnabled() {
    return impl != null && impl.isLoggable(tag, DEBUG);
  }

  /**
   * Formats the given message. If the last argument is an exception, we'll append the exception to
   * the end of the message.
   */
  private String formatMsg(String fmt, Object[] args) {
    StringBuilder sb = new StringBuilder();
    if (prefix != null) {
      sb.append(prefix);
      sb.append(" ");
    }
    Formatter formatter = new Formatter(sb, Locale.ENGLISH);
    try {
      formatter.format(fmt, args);
    } catch(Exception e) {
      return fmt; // ??
    } finally {
      formatter.close();
    }
    if (args != null && args.length >= 1 && args[args.length - 1] instanceof Throwable) {
      Throwable throwable = (Throwable) args[args.length - 1];
      StringWriter writer = new StringWriter();
      throwable.printStackTrace(new PrintWriter(writer));
      sb.append("\n");
      sb.append(writer.toString());
    }
    return sb.toString();
  }

  /** This interface is implemented by the client/server to provide the actual logging. */
  public interface LogImpl {
    boolean isLoggable(String tag, int level);
    void write(String tag, int level, String msg);
  }
}
