package au.com.codeka;


public class Cash {
    public static String format(double cash) {
        return "$"+NumberFormatter.format((long) Math.floor(cash));
    }
}
