package au.com.codeka.warworlds.server;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class StarSimulatorThreadManager {
  private static final Log log = new Log("StarSimulatorThreadManager");

  private final ArrayList<StarSimulatorThread> threads = new ArrayList<StarSimulatorThread>();
  private final Queue<Integer> starIDs = new ArrayDeque<Integer>();
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
            starIDs.add(res.getInt(1));
          }
        } catch (Exception e) {
          log.error("Error fetching starIDs to simulate.", e);
        }
      }

      if (starIDs.isEmpty()) {
        return 0;
      }
      return starIDs.remove();
    }
  }
}
