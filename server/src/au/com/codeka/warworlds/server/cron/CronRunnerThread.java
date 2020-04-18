package au.com.codeka.warworlds.server.cron;

import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.CronController;

import au.com.codeka.warworlds.server.model.CronJobDetails;

public class CronRunnerThread extends Thread {
  private static final Log log = new Log("CronRunnerThread");

  private static final CronRunnerThread thread = new CronRunnerThread();
  private boolean stopped;
  private final Object lock = new Object();

  /** Called at startup to begin the cron thread. */
  public static void setup() {
    thread.start();
  }

  public static void cleanup() {
    synchronized (thread.lock) {
      thread.stopped = true;
      thread.lock.notify();
    }

    try {
      thread.join(TimeUnit.SECONDS.toMicros(20)); // Wait 20 seconds.
    } catch (InterruptedException e) {
      log.error("Exception waiting for cron runner thread.", e);
    }
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
      long startTime = System.currentTimeMillis();
      jobDetails.setLastStatus(job.run(jobDetails.getParameters()));
      log.info(
          "Job '%s' complete in %dms", jobDetails.getAnnotation().name(),
          System.currentTimeMillis() - startTime);
    } catch (Throwable e) {
      log.error("Error running job.", e);
      jobDetails.setLastStatus(e.toString());
    }

    jobDetails.setLastRunTime(DateTime.now());

    // If it was a run-once then it's now disabled. Obviously.
    if (jobDetails.getRunOnce()) {
      log.info("Disabling run-once job now that's finished.");
      jobDetails.setEnabled(false);
    }
    try {
      new CronController().save(jobDetails);
    } catch (RequestException e) {
      log.error("Error saving cron job back to database.", e);
    }
  }

  /**
   * Called when you a cron job is edited, we might need to re-order what the next job will be.
   */
  public static void ping() {
    thread.interrupt();
  }

  @Override
  public void run() {
    while (!stopped) {
      try {
        List<CronJobDetails> jobs = new CronController().list();

        // Find the next job.
        CronJobDetails nextJob = null;
        for (CronJobDetails job : jobs) {
          if (!job.getEnabled()) {
            continue;
          }

          if (nextJob == null || nextJob.getNextRunTime().isAfter(job.getNextRunTime())) {
            nextJob = job;
          }
        }

        long sleepTime;
        if (nextJob == null) {
          log.info("No upcoming jobs.");
          sleepTime = TimeUnit.HOURS.toMillis(1);
        } else {
          sleepTime = nextJob.getNextRunTime().getMillis() - DateTime.now().getMillis();
          if (sleepTime < 0) {
            sleepTime = 0;
          }
          log.info("Next job: %s, in %d", nextJob.getAnnotation().name(), sleepTime);
        }

        try {
          synchronized (lock) {
            if (sleepTime == 0) {
              // As zero means "forever" in Java-land, we'll just wait 1 millisecond.
              sleepTime = 1;
            }
            lock.wait(sleepTime);
          }
        } catch(InterruptedException e) {
          log.info("Sleep interrupted, checking for new jobs.");
          continue;
        }

        if (nextJob != null) {
          runNow(nextJob);
        }
      } catch (Exception e) {
        log.error("Exception running cron job.", e);
      }
    }

    log.info("Cron thread has stopped.");
  }
}
