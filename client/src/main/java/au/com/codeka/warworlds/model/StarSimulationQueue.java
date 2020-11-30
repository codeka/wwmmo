package au.com.codeka.warworlds.model;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.SystemClock;

import androidx.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.Simulation;

/** Manages a queue of stars that need to be simulated. */
public class StarSimulationQueue {
  private static final Log log = new Log("StarSimulationQueue");
  public static final StarSimulationQueue i = new StarSimulationQueue();

  private Thread thread;
  private BlockingDeque<SimulateTask> enqueuedTasks = new LinkedBlockingDeque<>();

  /**
   * Schedules the given star to be simulated. We'll notify the StarManager's eventBus when
   * we finish.
   */
  public void simulate(Star star, boolean predict) {
    simulate(star, predict, null);
  }

  public void simulate(Star star, boolean predict, @Nullable Runnable completeCallback) {
    ensureThread();

    // do it so that the most recently-added star is the one we'll predict.
    enqueuedTasks.addFirst(new SimulateTask(star, predict, completeCallback));
  }

  /**
   * Determines whether the given star even needs a simulation. If it's been simulated in
   * the last 10 seconds, then it does not.
   */
  public static boolean needsSimulation(Star star) {
    if (star.getEmpirePresences() == null || star.getEmpirePresences().size() == 0) {
      return false;
    }

    // if it has our empire, and our empire's resource stats are 0, then it needs to be simulated.
    int myEmpireID = EmpireManager.i.getEmpire().getID();
    for (BaseEmpirePresence baseEmpirePresence : star.getEmpirePresences()) {
      EmpirePresence empirePresence = (EmpirePresence) baseEmpirePresence;
      if (empirePresence.getEmpireID() == myEmpireID) {
        if (Math.abs(empirePresence.getDeltaGoodsPerHour()) < 0.001f
            && Math.abs(empirePresence.getDeltaMineralsPerHour()) < 0.001f) {
          return true;
        }
      }
    }

    return star.getLastSimulation().isBefore(DateTime.now(DateTimeZone.UTC).minusSeconds(10));
  }

  private void ensureThread() {
    if (thread == null || !thread.isAlive()) {
      thread = new Thread(simulationRunnable);
      thread.start();
    }
  }

  private final Runnable simulationRunnable = () -> {
    while (true) {
      try {
        SimulateTask task = enqueuedTasks.take();
        Star star = task.star.get();
        if (star == null) {
          continue;
        }

        if (!needsSimulation(star)) {
          StarManager.eventBus.publish(star);
          continue;
        }

        DateTime lastSimulationTime = star.getLastSimulation();
        long startTime = SystemClock.elapsedRealtime();
        new Simulation(task.predict).simulate(star);
        long endTime = SystemClock.elapsedRealtime();

        StringBuilder sb = new StringBuilder();
        for (BaseEmpirePresence baseEmpirePresence : star.getEmpirePresences()) {
          sb.append(String.format(Locale.US, "%s->%.2f ", baseEmpirePresence.getEmpireKey(),
              baseEmpirePresence.getDeltaGoodsPerHour()));
        }
        log.info("Simulation of %d (%s) complete in %d ms (last simulation = %s) (Δ goods = %s)",
            star.getID(), star.getName(), endTime - startTime,
            TimeFormatter.create().format(lastSimulationTime), sb.toString().trim());

        if (task.completeCallback != null) {
          task.completeCallback.run();
        }
        StarManager.eventBus.publish(star);
      } catch (Exception e) {
        log.error("Exception caught simulating stars.", e);
        return; // we'll get restarted when a new star needs to be simulated.
      }
    }
  };

  private static class SimulateTask {
    public WeakReference<Star> star;
    public boolean predict;
    @Nullable public Runnable completeCallback;

    public SimulateTask(Star star, boolean predict, Runnable completeCallback) {
      this.star = new WeakReference<>(star);
      this.predict = predict;
      this.completeCallback = completeCallback;
    }
  }
}
