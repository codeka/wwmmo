package au.com.codeka.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import java.util.Locale;

/** This is a helper class for working with logging. We support both Android Log.x() and
 * Java's built-in logger (for server-side).
 */
public class Log {
    public static final int ERROR = 0;
    public static final int WARNING = 1;
    public static final int INFO = 2;
    public static final int DEBUG = 3;

    private static LogImpl mImpl;
    public static void setImpl(LogImpl impl) {
        mImpl = impl;
    }

    private String mTag;

    public Log(String tag) {
        mTag = tag;
    }

    public void error(String fmt, Object... args) {
        if (mImpl == null || !mImpl.isLoggable(mTag, ERROR)) {
            return;
        }
        mImpl.write(mTag, ERROR, formatMsg(fmt, args));
    }

    public void warning(String fmt, Object... args) {
        if (mImpl == null || !mImpl.isLoggable(mTag, WARNING)) {
            return;
        }
        mImpl.write(mTag, WARNING, formatMsg(fmt, args));
    }

    public void info(String fmt, Object... args) {
        if (mImpl == null || !mImpl.isLoggable(mTag, INFO)) {
            return;
        }
        mImpl.write(mTag, INFO, formatMsg(fmt, args));
    }

    public void debug(String fmt, Object... args) {
        if (mImpl == null || !mImpl.isLoggable(mTag, DEBUG)) {
            return;
        }
        mImpl.write(mTag, DEBUG, formatMsg(fmt, args));
    }

    /**
     * Formats the given message. If the last argument is an exception, we'll append
     * the exception to the end of the message.
     */
    private static String formatMsg(String fmt, Object[] args) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.ENGLISH);
        try {
            formatter.format(fmt, args);
        } finally {
            formatter.close();
        }
        if (args != null && args.length >= 1 && args[args.length - 1] instanceof Throwable) {
            Throwable throwable = (Throwable) args[args.length - 1];
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));
            sb.append(System.lineSeparator());
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
