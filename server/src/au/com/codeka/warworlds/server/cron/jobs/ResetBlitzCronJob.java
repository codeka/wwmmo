package au.com.codeka.warworlds.server.cron.jobs;

import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;

/**
 * This is a cron job that resets the entire universe and starts up a new one.
 */
@CronJob(
    name = "Reset Blitz",
    desc = "Resets the universe and starts up a new server mode for the month.")
public class ResetBlitzCronJob extends AbstractCronJob {
  @Override
  public String run(String extra) throws Exception {
    return "Blah";
  }
}
