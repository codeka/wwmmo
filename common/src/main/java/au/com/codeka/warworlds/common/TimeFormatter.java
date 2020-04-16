package au.com.codeka.warworlds.common;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormatter {
  private static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

  private int maxDays;
  private int minSeconds;
  private boolean alwaysIncludeMinutes;
  private boolean timeInPast;

  private TimeFormatter() {
    maxDays = 7;
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

  /**
   * Assume the time given is a time in the past. Instead of saying "2 days, 15 minutes" or
   * whatever, we'll say "2 days, 15 minutes ago". This will correctly handle maxDays and show a
   * date for a time a long time in the past (without the "ago" suffix).
   */
  public TimeFormatter withTimeInPast(boolean timeInPast) {
    this.timeInPast = timeInPast;
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
    String suffix = "";
    if (timeInPast) {
      millis = -millis;
      suffix = " ago";
    }

    long days = millis / Time.DAY;
    if (days > maxDays) {
      if (timeInPast) {
        millis = -millis;
      }

      long date = System.currentTimeMillis() + millis;
      return dateFormat.format(new Date(date));
    }

    long hours = (millis - (days * Time.DAY)) / Time.HOUR;
    long minutes = (millis - (days * Time.DAY) - (hours * Time.HOUR)) / Time.MINUTE;
    if (days > 0) {
      if (alwaysIncludeMinutes) {
        return String.format(
            Locale.ENGLISH,
            "%d day%s, %d hr%s %d min%s%s",
            days, days == 1 ? "" : "s",
            hours, hours == 1 ? "" : "s",
            minutes, minutes == 1 ? "" : "s",
            suffix);
      } else {
        return String.format(
            Locale.ENGLISH,
            "%d day%s, %d hr%s%s",
            days, days == 1 ? "" : "s",
            hours, hours == 1 ? "" : "s",
            suffix);
      }
    } else if (hours > 3 && !alwaysIncludeMinutes) {
      return String.format(Locale.ENGLISH, "%d hrs%s", hours, suffix);
    } else if (hours > 0) {
      return String.format(
          Locale.ENGLISH,
          "%d hr%s, %d min%s%s",
          hours, hours == 1 ? "" : "s",
          minutes, minutes == 1 ? "" : "s",
          suffix);
    } else if (minutes <= 0) {
      if (timeInPast) {
        return "just now";
      } else {
        return "< 1 min";
      }
    } else {
      return String.format(
          Locale.ENGLISH,
          "%d min%s%s",
          minutes, millis == 1 ? "" : "s", suffix);
    }
  }
}
