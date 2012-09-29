package au.com.codeka;

import android.util.FloatMath;

public class TimeInHours {
    public static String format(float timeInHours) {
        if (timeInHours > 0.0f) {
            int hrs = (int) FloatMath.floor(timeInHours);
            int mins = (int) FloatMath.floor((timeInHours - hrs) * 60.0f);

            if (hrs == 0) {
                return String.format("%d min%s", mins, (mins == 1 ? "" : "s"));
            } else {
                return String.format("%d hr%s, %d min%s",
                                     hrs, (hrs == 1 ? "" : "s"),
                                     mins, (mins == 1 ? "" : "s"));
            }
        } else {
            return "???";
        }
    }
}
