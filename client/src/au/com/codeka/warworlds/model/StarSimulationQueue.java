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
    private BlockingQueue<Star> mEnqueuedStars = new LinkedBlockingQueue<Star>();

    /** Schedules the given star to be simulated. We'll notify the StarManager's eventBus when
        we finish. */
    public void simulate(Star star) {
        ensureThread();
        mEnqueuedStars.add(star);
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
                    Star star = mEnqueuedStars.take();
                    log.info("Simulating star %s...", star.getName());
                    new Simulation().simulate(star);
                    StarManager.eventBus.publish(star);
                    log.info("Simulation of %s complete.", star.getName());
                } catch(Exception e) {
                    log.error("Exception caught simulating stars.", e);
                    return; // we'll get restarted when a new star needs to be simulated.
                }
            }
        }
    };
}
