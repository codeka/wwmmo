package au.com.codeka;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;

import android.util.FloatMath;

public class TimeInHours {
    public static String format(float timeInHours) {
        if (timeInHours > 0.0f) {
            int hrs = (int) FloatMath.floor(timeInHours);
            int mins = (int) FloatMath.floor((timeInHours - hrs) * 60.0f);

            return format(new Period(hrs, mins, 0, 0));
        } else {
            return "???";
        }
    }

    /**
     * Formats the time between now and then (we assume then is chronologically before now) as
     * an "hrs/mins" string.
     */
    public static String format(DateTime now, DateTime then) {
        if (now.isBefore(then)) {
            DateTime tmp = now;
            now = then;
            then = tmp;
        }

        Duration d = new Interval(then, now).toDuration();
        if (d.compareTo(Duration.standardSeconds(5)) <  0)
            d = Duration.standardSeconds(5);

        return format(d.toPeriod());
    }

    public static String format(Duration duration) {
        return format(duration.toPeriod());
    }

    public static String format(Period p) {
        int days = p.toStandardDays().getDays();
        if (days > 0) {
            return String.format("%d day%s, %d hr%s, %d min%s",
                                 days, days == 1 ? "" : "s",
                                 p.getHours(), p.getHours() == 1 ? "" : "s",
                                 p.getMinutes(), p.getMinutes() == 1 ? "" : "s");
        } else if (p.getHours() > 0) {
            return String.format("%d hr%s, %d min%s",
                    p.getHours(), p.getHours() == 1 ? "" : "s",
                    p.getMinutes(), p.getMinutes() == 1 ? "" : "s");
        } else {
            return String.format("%d min%s",
                    p.getMinutes(), p.getMinutes() == 1 ? "" : "s");
        }
    }
}
