package au.com.codeka.warworlds.server.store;

import org.joda.time.DateTime;

/**
 * Helper class for some stats stuff.
 */
class StatsHelper {
  /**
   * Take a stamp in unix-epoch-millis format (i.e. like what you'd get from
   * {@code System.currentTimeMillis()}, and return a "day" integer of the form yyyymmdd.
   */
  public static int timestampToDay(long timestamp) {
    DateTime dt = new DateTime(timestamp);
    return dateTimeToDay(dt);
  }

  public static int dateTimeToDay(DateTime dt) {
    return dt.year().get() * 10000 + dt.monthOfYear().get() * 100 + dt.dayOfMonth().get();
  }
}
