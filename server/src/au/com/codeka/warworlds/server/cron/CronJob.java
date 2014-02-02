package au.com.codeka.warworlds.server.cron;

/**
 * This is the base class for all cron jobs. 
 */
public abstract class CronJob {
    public abstract void run(String extra) throws Exception;

    protected static int extraToNum(String extra, int minNumber, int defaultNumber) {
        int num = defaultNumber;
        if (extra != null) {
            num = Integer.parseInt(extra);
        }
        if (num < minNumber) {
            num = minNumber;
        }
        return num;
    }
}
