package au.com.codeka;


public class Cash {
    public static String format(float cash) {
        return "$"+NumberFormatter.format((int) Math.floor(cash));
    }
}
