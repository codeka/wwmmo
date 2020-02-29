package au.com.codeka.warworlds.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class StarSimulatorThreadManager {
  public static final StarSimulatorThreadManager i = new StarSimulatorThreadManager();

  // Time, in milliseconds, between logs of the simulation stats.
  private static final long STATS_LOG_DELAY_MS = 10000;

  private static final Log log = new Log("StarSimulatorThreadManager");

  private final ArrayList<StarSimulatorThread> threads = new ArrayList<>();
  private final Queue<Integer> starIDs = new ArrayDeque<>();
  private final Set<Integer> lastStarIDs = new HashSet<>();
  private final Object lock = new Object();
  private boolean stopped;
  private final Thread monitorThread = new Thread(this::threadMonitor);

  public void start() {
    stopped = false;
    for (int i = 0; i < Configuration.i.getNumStarSimulationThreads(); i++) {
      StarSimulatorThread thread = new StarSimulatorThread(this);
      thread.start();
      threads.add(thread);
    }
    monitorThread.start();
    log.info("Started %d star simulation threads.", threads.size());
  }

  public void stop() {
    stopped = true;
    // Note: we don't wait for the monitor thread to stop, don't really care if it goes for a bit
    // longer, it'll stop itself when it notices stopped is false.
    for (StarSimulatorThread thread : threads) {
      thread.stop();
    }
    starIDs.clear();
  }

  public boolean hasMoreStarsToSimulate() {
    synchronized (lock) {
      return !starIDs.isEmpty();
    }
  }

  /** Returns the ID of the next star to simulate. */
  public int getNextStar() {
    synchronized(lock) {
      if (starIDs.isEmpty()) {
        // Grab 50 stars at a time, to save all those queries.
        String sql =
            "SELECT id FROM stars WHERE empire_count > 0 ORDER BY last_simulation ASC LIMIT 50";
        try (SqlStmt stmt = DB.prepare(sql)) {
          SqlResult res = stmt.select();
          while (res.next()) {
            int starID = res.getInt(1);
            // If this starID was handed out in the last set, it's possible that another thread
            // is still simulating it. Ignore it for now, and wait for the next time around.
            if (lastStarIDs.contains(starID)) {
              continue;
            }
            starIDs.add(starID);
          }
        } catch (Exception e) {
          log.error("Error fetching starIDs to simulate.", e);
        }

        // clear out the lastStarIDs set and start afresh with this new batch.
        lastStarIDs.clear();
      }

      if (starIDs.isEmpty()) {
        log.info("Got an empty set, no stars to simulate.");
        return 0;
      }
      int starID = starIDs.remove();
      lastStarIDs.add(starID);
      return starID;
    }
  }

  /**
   * A function that runs in it's own thread whose job is to monitor the star simulation thread(s),
   * periodically log their status, and make sure they're not stuck.
   */
  private void threadMonitor() {
    while (!stopped) {
      int i = 0;
      for (StarSimulatorThread thread : threads) {
        StarSimulatorThread.ProcessingStats stats = thread.stats();
        if (stats.numStars == 0 && stats.currentStar == null) {
          // Nothing interesting to report.
          continue;
        }

        String currStarMsg = stats.currentStar == null
            ? "(no current star)"
            : String.format(Locale.ENGLISH,
                "current star: [%d] %s for %dms",
                stats.currentStar.getID(),
                stats.currentStar.getName(),
                stats.currentStarProcessingTime);
        log.info("[%d] %d stars, %dms total, %d in db, %s",
            i, stats.numStars, stats.totalTimeMs, stats.dbTimeMs, currStarMsg);

        // TODO: if it appears currentStar is stuck, do something...

        i++;
      }

      try {
        Thread.sleep(STATS_LOG_DELAY_MS);
      } catch (InterruptedException e) {
        // Ignore.
      }
    }
  }
}
