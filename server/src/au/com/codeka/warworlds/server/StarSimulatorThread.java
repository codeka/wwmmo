package au.com.codeka.warworlds.server;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Star;

/**
 * This is a background thread that runs every 2 seconds and simulates the oldest star in the
 * system. This ensures we never let our stars get TOO out-of-date.
 */
public class StarSimulatorThread {
  private static final Log log = new Log("StarSimulatorThread");
  private Thread thread;
  private boolean stopped;
  private final StarSimulatorThreadManager manager;

  private final Object statsLock = new Object();
  private ProcessingStats stats = new ProcessingStats();

  private static final int WAIT_TIME_NO_STARS = 10 * 60 * 1000; // 10 minutes, after no stars found
  private static final int WAIT_TIME_ERROR = 60 * 1000; // 1 minute, in case of error
  private static final int WAIT_TIME_NORMAL = 0; // don't wait if there's more stars to simulate

  public StarSimulatorThread(StarSimulatorThreadManager manager) {
    this.manager = manager;
  }

  public void start() {
    if (thread != null) {
      stop();
    }

    thread = new Thread(this::threadProc);
    thread.setDaemon(true);
    thread.setName("Star-Simulator");
    thread.setPriority(Thread.NORM_PRIORITY - 1);
    thread.start();
  }

  public void stop() {
    if (thread == null) {
      return;
    }

    stopped = true;
    try {
      thread.interrupt();
      thread.join(TimeUnit.SECONDS.toMicros(10)); // Wait up to 20 seconds.
    } catch (InterruptedException e) {
      // ignore
    }

    thread = null;
  }

  /**
   * Returns a snapshots of stats about what we've been doing since the last time you called this.
   */
  public ProcessingStats stats() {
    if (thread == null) {
      log.error("Thread is null, attempting to restarting.");
      start();
    }

    if (!thread.isAlive() || thread.isInterrupted()) {
      log.error(
          "Thread.isAlive()=%s thread.isInterrupted()=%s, attempting to restart.",
          thread.isAlive(), thread.isInterrupted());
      thread = null;
      start();
    }

    synchronized (statsLock) {
      ProcessingStats currStats = stats;
      stats = new ProcessingStats();

      if (currStats.currentStar != null) {
        currStats.currentStarProcessingTime =
            System.currentTimeMillis() - currStats.currentStarProcessingTime;
      }

      return currStats;
    }
  }

  private void threadProc() {
    try {
      int numSimulatedSinceEventProcessorPinged = 0;
      while (!stopped) {
        int waitTimeMs = simulateOneStar();
        numSimulatedSinceEventProcessorPinged++;

        if (numSimulatedSinceEventProcessorPinged >= 50) {
          EventProcessor.i.ping();
          numSimulatedSinceEventProcessorPinged = 0;
        }

        if (waitTimeMs > 0) {
          if (waitTimeMs > WAIT_TIME_ERROR) {
            waitTimeMs = WAIT_TIME_ERROR;
          }
          log.debug(String.format(
              Locale.US,
              "Waiting %d seconds before simulating next star.",
              waitTimeMs / 1000));
          synchronized (statsLock) {
            stats.idleTimeMs += waitTimeMs;
          }
          try {
            Thread.sleep(waitTimeMs);
          } catch (InterruptedException e) {
            // Ignore.
          }
        } else {
          Thread.yield();
        }
      }
    } catch (Throwable e) {
      log.error("Error in star simulation thread!");
      thread = null;
    }
  }

  private int simulateOneStar() {
    try (Transaction t = DB.beginTransaction()) {
      int starID = manager.getNextStar();
      if (starID == 0) {
        return WAIT_TIME_NO_STARS;
      }
      log.debug("Simulating star: " + starID);
      long startTime = System.currentTimeMillis();

      Star star = new StarController(t).getStar(starID);
      synchronized (statsLock) {
        stats.currentStar = star;
        stats.currentStarProcessingTime = System.currentTimeMillis();
      }
      if (star.getLastSimulation().isAfter(DateTime.now().minusHours(1))) {
        t.commit();

        if (manager.hasMoreStarsToSimulate()) {
          // if there's more cached stars, just try again immediately.
          return 0;
        }

        // how long would we have to wait (in seconds) before there WOULD HAVE been one hour since
        // it was last simulated?
        int seconds = Seconds
            .secondsBetween(DateTime.now().minusHours(1), star.getLastSimulation()).getSeconds();
        return seconds * 1000;
      }

      new Simulation().simulate(star);
      long simulateEndTime = System.currentTimeMillis();
      // we don't ping event processor now, because we rather do it once every 50 stars or so.
      new StarController(t).update(star, false);

      long endTime = System.currentTimeMillis();
      synchronized (statsLock) {
        stats.numStars++;
        stats.totalTimeMs += endTime - startTime;
        stats.dbTimeMs += endTime - simulateEndTime;
      }

      t.commit();
      return WAIT_TIME_NORMAL;
    } catch (Throwable e) {
      log.error("Exception caught simulating star!", e);
      // TODO: if there are errors, it'll just keep reporting over and over... probably a good thing
      // because we'll definitely need to fix it!
      return WAIT_TIME_ERROR;
    } finally {
      synchronized (statsLock) {
        stats.currentStar = null;
      }
    }
  }

  static class ProcessingStats {
    // The number of stars we've processed since the last stats call.
    int numStars;

    // The total amount of time we've spent processing stars since the last stats call.
    long totalTimeMs;

    // The total amount of time we've spent in the database since the last stats call.
    long dbTimeMs;

    // Amount of time the thread was idle, in milliseconds.
    long idleTimeMs;

    // If not null, the star we're processing at the time you call the stats method.
    @Nullable
    Star currentStar;

    // If currentStar is non-null, the amount of time we've been spending on currentStar so far.
    long currentStarProcessingTime;
  }
}
