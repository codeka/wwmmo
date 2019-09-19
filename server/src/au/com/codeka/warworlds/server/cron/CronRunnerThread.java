package au.com.codeka.warworlds.server.cron;

import java.util.concurrent.TimeUnit;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.cron.jobs.UpdateDashboardCronJob;
import au.com.codeka.warworlds.server.cron.jobs.UpdateRanksCronJob;

public class CronRunnerThread extends Thread {
  private static final Log log = new Log("CronRunnerThread");

  /** Called at startup to begin the cron thread. */
  public static void setup() {
    CronRunnerThread thread = new CronRunnerThread();
    thread.start();
  }

  @Override
  public void run() {
    while (true) {
      try {
        new UpdateRanksCronJob().run("");
        new UpdateDashboardCronJob().run("");
      } catch (Exception e) {
        log.error("Error running jobs.", e);
      }

      try {
        Thread.sleep(TimeUnit.HOURS.toMillis(1));
      } catch (InterruptedException e) {
        return;
      }
    }
  }
}
