package au.com.codeka.warworlds.common;

import java.util.Date;
import java.util.Locale;

public class TimeFormatter {
  private int maxDays;
  private int minSeconds;
  private boolean alwaysIncludeMinutes;

  private TimeFormatter() {
    maxDays = 2;
    minSeconds = 15;
    alwaysIncludeMinutes = false;
  }

  public static TimeFormatter create() {
    return new TimeFormatter();
  }

  public TimeFormatter withMaxDays(int maxDays) {
    this.maxDays = maxDays;
    return this;
  }

  public TimeFormatter withMinSeconds(int minSeconds) {
    this.minSeconds = minSeconds;
    return this;
  }

  /**
   * Returns a {@link TimeFormatter} that will always include minute-level precision in the output,
   * no matter how many days/hours it is. The default is to only include minute-level precision if
   * there is less than three hours.
   */
  public TimeFormatter withAlwaysIncludeMinutes(boolean alwaysIncludeMinutes) {
    this.alwaysIncludeMinutes = alwaysIncludeMinutes;
    return this;
  }

  /** Format the given time in hours. */
  public String format(float timeInHours) {
    return format((long)(timeInHours * Time.HOUR));
  }

  /**
   * Formats the time between now and then (we assume then is chronologically
   * before now) as an "hrs/mins" string.
   */
  public String format(Date now, Date then) {
    return format(then.getTime() - now.getTime());
  }

  public String format(long millis) {
    long days = millis / (24 * Time.HOUR);
    long hours = (millis / Time.HOUR) - (days * 24 * Time.HOUR);
    long minutes = (millis / Time.MINUTE) - (days * 24 * Time.HOUR) - (hours * Time.HOUR);
    if (days > 0) {
      if (alwaysIncludeMinutes) {
        return String.format(
            Locale.ENGLISH,
            "%d day%s, %d hr%s %d min%s",
            days, days == 1 ? "" : "s",
            hours, hours == 1 ? "" : "s",
            minutes, minutes == 1 ? "" : "s");
      } else {
        return String.format(
            Locale.ENGLISH,
            "%d day%s, %d hr%s",
            days, days == 1 ? "" : "s",
            hours, hours == 1 ? "" : "s");
      }
    } else if (hours > 3 && !alwaysIncludeMinutes) {
      return String.format(Locale.ENGLISH, "%d hrs", hours);
    } else if (hours > 0) {
      return String.format(
          Locale.ENGLISH,
          "%d hr%s, %d min%s",
          hours, hours == 1 ? "" : "s",
          minutes, minutes == 1 ? "" : "s");
    } else if (minutes <= 0) {
      return "just now";
    } else {
      return String.format(
          Locale.ENGLISH,
          "%d min%s",
          minutes, millis == 1 ? "" : "s");
    }
  }
}
