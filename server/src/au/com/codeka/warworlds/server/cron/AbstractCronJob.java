package au.com.codeka.warworlds.server.cron;

/**
 * This is the base class for all cron jobs.
 */
public abstract class AbstractCronJob {
  public abstract void run(String extra) throws Exception;
}
