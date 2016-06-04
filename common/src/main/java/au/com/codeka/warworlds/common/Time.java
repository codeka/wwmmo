package au.com.codeka.warworlds.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Some helper properties to do with time.
 */
public class Time {
  public static final long SECOND = 1000L;
  public static final long MINUTE = 60 * SECOND;
  public static final long HOUR = 60 * MINUTE;
  public static final long DAY = 24 * HOUR;

  private static final SimpleDateFormat formatter =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

  public static float toHours(long time) {
    return (float) time / HOUR;
  }

  public static String format(long time) {
    return formatter.format(new Date(time));
  }
}
