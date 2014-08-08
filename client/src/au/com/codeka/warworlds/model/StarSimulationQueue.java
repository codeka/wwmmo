package au.com.codeka.warworlds.model;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.Simulation;

/** Manages a queue of stars that need to be simulated. */
public class StarSimulationQueue {
    private static final Log log = new Log("StarSimulationQueue");
    public static final StarSimulationQueue i = new StarSimulationQueue();

    private Thread mThread;
    private BlockingDeque<SimulateTask> enqueuedTasks = new LinkedBlockingDeque<SimulateTask>();

    /** Schedules the given star to be simulated. We'll notify the StarManager's eventBus when
        we finish. */
    public void simulate(Star star, boolean predict) {
        ensureThread();

        // do it so that the most recently-added star is the one we'll predict.
        enqueuedTasks.addFirst(new SimulateTask(star, predict));
    }

    /** Determines whether the given star even needs a simulation. If it's been simulated in
     *  the last 5 minutes, then it does not. */
    public static boolean needsSimulation(Star star) {
        if (star.getEmpirePresences() == null
                || star.getEmpirePresences().size() == 0) {
            return false;
        }
        return star.getLastSimulation().isBefore(DateTime.now(DateTimeZone.UTC).minusMinutes(5));
    }

    private void ensureThread() {
        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(mSimulationRunnable);
            mThread.start();
        }
    }

    private Runnable mSimulationRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    SimulateTask task = enqueuedTasks.take();
                    Star star = task.star.get();
                    if (star == null) {
                        continue;
                    }

                    if (!needsSimulation(star)) {
                        continue;
                    }

                    log.info("Simulating star #%d %s...", star.getID(), star.getName());
                    new Simulation(task.predict).simulate(star);
                    StarManager.eventBus.publish(star);
                    log.info("Simulation of %s complete.", star.getID(), star.getName());
                } catch(Exception e) {
                    log.error("Exception caught simulating stars.", e);
                    return; // we'll get restarted when a new star needs to be simulated.
                }
            }
        }
    };

    private static class SimulateTask {
        public WeakReference<Star> star;
        public boolean predict;

        public SimulateTask(Star star, boolean predict) {
            this.star = new WeakReference<Star>(star);
            this.predict = predict;
        }
    }
}
