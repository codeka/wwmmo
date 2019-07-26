package au.com.codeka.warworlds.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class StarSimulatorThreadManager {
  private static final Log log = new Log("StarSimulatorThreadManager");

  private final ArrayList<StarSimulatorThread> threads = new ArrayList<>();
  private final Queue<Integer> starIDs = new ArrayDeque<>();
  private final Set<Integer> lastStarIDs = new HashSet<>();
  private final Object lock = new Object();

  public void start() {
    for (int i = 0; i < Configuration.i.getNumStarSimulationThreads(); i++) {
      StarSimulatorThread thread = new StarSimulatorThread(this);
      thread.start();
      threads.add(thread);
    }
    log.info("Started %d star simulation threads.", threads.size());
  }

  public void stop() {
    for (StarSimulatorThread thread : threads) {
      thread.stop();
    }
  }

  /** Returns the ID of the next star to simulate. */
  public int getNextStar() {
    synchronized(lock) {
      if (starIDs.isEmpty()) {
        // Grab 50 stars at a time, to save all those queries.
        String sql = "SELECT id FROM stars" + " WHERE empire_count > 0"
            + " ORDER BY last_simulation ASC LIMIT 50";
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
        return 0;
      }
      int starID = starIDs.remove();
      lastStarIDs.add(starID);
      return starID;
    }
  }
}
