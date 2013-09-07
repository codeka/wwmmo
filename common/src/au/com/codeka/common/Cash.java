package au.com.codeka.common;

import java.text.NumberFormat;
import java.util.Locale;

public class Cash {
    public static String format(float cash) {
        String s = NumberFormat.getInstance(new Locale("en_US")).format(
            (int) Math.floor(cash));
        return "$"+s;
    }
}
