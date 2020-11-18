package au.com.codeka;


import au.com.codeka.common.NumberFormatter;

public class Cash {
    public static String format(double cash) {
        return "$"+ NumberFormatter.format((long) Math.floor(cash));
    }
}
