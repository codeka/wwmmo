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
  private static final int ERROR = 0;
  private static final int WARNING = 1;
  private static final int INFO = 2;
  private static final int DEBUG = 3;

  private static LogImpl impl;
  public static void setImpl(LogImpl impl) {
    Log.impl = impl;
  }

  @Nullable private LogHook hook;
  private String tag;
  @Nullable private String prefix;

  /** Create a {@link Log} that'll log with the given tag. */
  public Log(String tag) {
    this.tag = tag;
  }

  /** Create a {@link Log} that will write to the given {@link LogHook} instead of the log file. */
  public Log(LogHook hook) {
    this.tag = "Log";
    this.hook = hook;
  }

  public void setPrefix(@Nullable String prefix) {
    this.prefix = prefix;
  }

  public void error(String fmt, Object... args) {
    write(ERROR, fmt, args);
  }

  public void warning(String fmt, Object... args) {
    write(WARNING, fmt, args);
  }

  public void info(String fmt, Object... args) {
    write(INFO, fmt, args);
  }

  public void debug(String fmt, Object... args) {
    write(DEBUG, fmt, args);
  }

  private void write(int level, String fmt, Object... args) {
    if (hook != null) {
      hook.write(formatMsg(fmt, args));
      return;
    }

    if (impl == null || !impl.isLoggable(tag, level)) {
      return;
    }
    impl.write(tag, level, formatMsg(fmt, args));
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
    try (Formatter formatter = new Formatter(sb, Locale.ENGLISH)) {
      formatter.format(fmt, args);
    } catch(Exception e) {
      return fmt; // ??
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

  /** You can implement this interface if you want to hook into the log messages. */
  public interface LogHook {
    void write(String msg);
  }
}
