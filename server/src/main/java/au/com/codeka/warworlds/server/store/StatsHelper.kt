package au.com.codeka.warworlds.server.store

import org.joda.time.DateTime

/**
 * Helper class for some stats stuff.
 */
internal object StatsHelper {
  /**
   * Take a stamp in unix-epoch-millis format (i.e. like what you'd get from
   * `System.currentTimeMillis()`, and return a "day" integer of the form yyyymmdd.
   */
  fun timestampToDay(timestamp: Long): Int {
    val dt = DateTime(timestamp)
    return dateTimeToDay(dt)
  }

  fun dateTimeToDay(dt: DateTime): Int {
    return dt.year().get() * 10000 + dt.monthOfYear().get() * 100 + dt.dayOfMonth().get()
  }
}