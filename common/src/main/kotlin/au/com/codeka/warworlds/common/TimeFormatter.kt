package au.com.codeka.warworlds.common

import java.text.DateFormat
import java.util.*

class TimeFormatter private constructor() {
  private var maxDays = 7
  private var minSeconds = 15
  private var alwaysIncludeMinutes = false
  private var timeInPast = false

  fun withMaxDays(maxDays: Int): TimeFormatter {
    this.maxDays = maxDays
    return this
  }

  fun withMinSeconds(minSeconds: Int): TimeFormatter {
    this.minSeconds = minSeconds
    return this
  }

  /**
   * Returns a [TimeFormatter] that will always include minute-level precision in the output,
   * no matter how many days/hours it is. The default is to only include minute-level precision if
   * there is less than three hours.
   */
  fun withAlwaysIncludeMinutes(alwaysIncludeMinutes: Boolean): TimeFormatter {
    this.alwaysIncludeMinutes = alwaysIncludeMinutes
    return this
  }

  /**
   * Assume the time given is a time in the past. Instead of saying "2 days, 15 minutes" or
   * whatever, we'll say "2 days, 15 minutes ago". This will correctly handle maxDays and show a
   * date for a time a long time in the past (without the "ago" suffix).
   */
  fun withTimeInPast(timeInPast: Boolean): TimeFormatter {
    this.timeInPast = timeInPast
    return this
  }

  /** Format the given time in hours.  */
  fun format(timeInHours: Float): String {
    return format((timeInHours * Time.HOUR).toLong())
  }

  /**
   * Formats the time between now and then (we assume then is chronologically
   * before now) as an "hrs/mins" string.
   */
  fun format(now: Date, then: Date): String {
    return format(then.time - now.time)
  }

  fun format(millis: Long): String {
    var millis = millis
    var suffix = ""
    if (timeInPast) {
      millis = -millis
      suffix = " ago"
    }
    val days = millis / Time.DAY
    if (days > maxDays) {
      if (timeInPast) {
        millis = -millis
      }
      val date = System.currentTimeMillis() + millis
      return dateFormat.format(Date(date))
    }
    val hours = (millis - days * Time.DAY) / Time.HOUR
    val minutes = (millis - days * Time.DAY - hours * Time.HOUR) / Time.MINUTE
    return if (days > 0) {
      if (alwaysIncludeMinutes) {
        String.format(
            Locale.ENGLISH,
            "%d day%s, %d hr%s %d min%s%s",
            days, if (days == 1L) "" else "s",
            hours, if (hours == 1L) "" else "s",
            minutes, if (minutes == 1L) "" else "s",
            suffix)
      } else {
        String.format(
            Locale.ENGLISH,
            "%d day%s, %d hr%s%s",
            days, if (days == 1L) "" else "s",
            hours, if (hours == 1L) "" else "s",
            suffix)
      }
    } else if (hours > 3 && !alwaysIncludeMinutes) {
      String.format(Locale.ENGLISH, "%d hrs%s", hours, suffix)
    } else if (hours > 0) {
      String.format(
          Locale.ENGLISH,
          "%d hr%s, %d min%s%s",
          hours, if (hours == 1L) "" else "s",
          minutes, if (minutes == 1L) "" else "s",
          suffix)
    } else if (minutes <= 0) {
      if (timeInPast) {
        "just now"
      } else {
        "< 1 min"
      }
    } else {
      String.format(
          Locale.ENGLISH,
          "%d min%s%s",
          minutes, if (millis == 1L) "" else "s", suffix)
    }
  }

  companion object {
    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    fun create(): TimeFormatter {
      return TimeFormatter()
    }
  }

}