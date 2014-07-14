package au.com.codeka.warworlds.model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.Simulation;

/** Manages a queue of stars that need to be simulated. */
public class StarSimulationQueue {
    private static final Log log = new Log("StarSimulationQueue");
    public static final StarSimulationQueue i = new StarSimulationQueue();

    private Thread mThread;
    private BlockingQueue<SimulateTask> enqueuedTasks = new LinkedBlockingQueue<SimulateTask>();

    /** Schedules the given star to be simulated. We'll notify the StarManager's eventBus when
        we finish. */
    public void simulate(Star star, boolean predict) {
        ensureThread();
        enqueuedTasks.add(new SimulateTask(star, predict));
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
                    log.info("Simulating star %s...", task.star.getName());
                    new Simulation(task.predict).simulate(task.star);
                    StarManager.eventBus.publish(task.star);
                    log.info("Simulation of %s complete.", task.star.getName());
                } catch(Exception e) {
                    log.error("Exception caught simulating stars.", e);
                    return; // we'll get restarted when a new star needs to be simulated.
                }
            }
        }
    };

    private static class SimulateTask {
        public Star star;
        public boolean predict;

        public SimulateTask(Star star, boolean predict) {
            this.star = star;
            this.predict = predict;
        }
    }
}
