package au.com.codeka.warworlds.server.cron;

import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.cron.jobs.UpdateDashboardCronJob;
import au.com.codeka.warworlds.server.cron.jobs.UpdateRanksCronJob;
import au.com.codeka.warworlds.server.ctrl.CronController;    // TODO: save details of the run to the database.

import au.com.codeka.warworlds.server.model.CronJobDetails;

public class CronRunnerThread extends Thread {
  private static final Log log = new Log("CronRunnerThread");

  /** Called at startup to begin the cron thread. */
  public static void setup() {
    CronRunnerThread thread = new CronRunnerThread();
    thread.start();
  }

  public static synchronized void runNow(CronJobDetails jobDetails) {
    log.info("Running job '%s' class=%s param='%s' now",
        jobDetails.getAnnotation().name(), jobDetails.getClassName(), jobDetails.getParameters());
    AbstractCronJob job;
    try {
      job = jobDetails.createInstance();
    } catch (IllegalAccessException | InstantiationException e) {
      log.error("Could not create instance of job.", e);
      return;
    }

    try {
      jobDetails.setLastStatus(job.run(jobDetails.getParameters()));
    } catch (Exception e) {
      log.error("Error running job.", e);
      jobDetails.setLastStatus(e.toString());
    }

    jobDetails.setLastRunTime(DateTime.now());
    try {
      new CronController().save(jobDetails);
    } catch (RequestException e) {
      log.error("Error saving cron job back to database.", e);
    }
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
