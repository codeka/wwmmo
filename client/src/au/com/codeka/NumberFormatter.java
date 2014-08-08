package au.com.codeka;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/** Helper for formatting numbers in a nice way (with commas, essentially). */
public class NumberFormatter {
    private static NumberFormat format;

    static {
        format = NumberFormat.getInstance();
        ((DecimalFormat) format).applyPattern("#,##0;-#,##0");
    }

    public static String format(int number) {
        return format.format(number);
    }

    public static String format(long number) {
        return format.format(number);
    }

    public static String format(double number) {
        return format.format(Math.ceil(number));
    }

    public static String format(float number) {
        return format.format(Math.ceil(number));
    }
}
