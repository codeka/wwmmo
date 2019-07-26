package au.com.codeka.warworlds.server;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.ctrl.StarController;
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
      thread.join();
    } catch (InterruptedException e) {
      // ignore
    }

    thread = null;
  }

  private void threadProc() {
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
        log.info(String.format(
            Locale.US,
            "Waiting %d seconds before simulating next star.",
            waitTimeMs / 1000));
        try {
          Thread.sleep(waitTimeMs);
        } catch (InterruptedException e) {
          // Ignore.
        }
      } else {
        Thread.yield();
      }
    }
  }

  private int simulateOneStar() {
    try {
      int starID = manager.getNextStar();
      if (starID == 0) {
        return WAIT_TIME_NO_STARS;
      }
      log.debug("Simulating star: " + starID);
      long startTime = System.currentTimeMillis();

      Star star = new StarController().getStar(starID);
      if (star.getLastSimulation().isAfter(DateTime.now().minusHours(1))) {
        // how long would we have to wait (in seconds) before there WOULD HAVE been one hour since
        // it was last simulated?
        int seconds = Seconds
            .secondsBetween(DateTime.now().minusHours(1), star.getLastSimulation()).getSeconds();
        return seconds * 1000;
      }

      new Simulation().simulate(star);
      long simulateEndTime = System.currentTimeMillis();
      // we don't ping event processor now, because we rather do it once every 50 stars or so.
      new StarController().update(star, false);

      long endTime = System.currentTimeMillis();
      log.info(String.format(Locale.US,
          "Simulated star (%d colonies, %d fleets) in %dms (%dms in DB): \"%s\" [%d]", star
              .getColonies().size(), star.getFleets().size(), endTime - startTime, endTime
              - simulateEndTime, star.getName(), star.getID()));
      return WAIT_TIME_NORMAL;
    } catch (Exception e) {
      log.error("Exception caught simulating star!", e);
      // TODO: if there are errors, it'll just keep reporting
      // over and over... probably a good thing because we'll
      // definitely need to fix it!
      return WAIT_TIME_ERROR;
    }
  }
}
