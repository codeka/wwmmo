package au.com.codeka.warworlds.server;

import java.sql.ResultSet;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.cron.SimulateAllStarsCronJob;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Star;

/**
 * This is a background thread that runs every 2 seconds and simulates
 * the oldest star in the system. This ensures we never let our stars get
 * TOO out-of-date.
 */
public class StarSimulatorThread {
    private static final Logger log = LoggerFactory.getLogger(StarSimulatorThread.class);
    private Thread mThread;
    private boolean mStopped;

    public void start() {
        if (mThread != null) {
            stop();
        }

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                threadproc();
            }
        });
        mThread.setDaemon(true);
        mThread.setName("Star-Simulator");
        mThread.setPriority(Thread.NORM_PRIORITY - 1);
        mThread.start();
    }

    public void stop() {
        if (mThread == null) {
            return;
        }

        mStopped = true;
        try {
            mThread.interrupt();
            mThread.join();
        } catch (InterruptedException e) {
            // ignore
        }

        mThread = null;
    }

    private void threadproc() {
        while (!mStopped) {
            simulateOneStar();

            // TODO: if we ever catch up, sleep for more than 2 seconds!
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void simulateOneStar() {
        try {
            int starID = findOldestStar();
            log.info("Simulating star: "+starID);

            Star star = new StarController().getStar(starID);
            if (star.getLastSimulation().isAfter(DateTime.now().minusHours(1))) {
                // TODO: sleep for more than 2 seconds if we get here...
                return;
            }

            new Simulation().simulate(star);
            new StarController().update(star);
        } catch (Exception e) {
            log.info("HERE");
            log.error("Exception caught simulating star!", e);
            // TODO: if there are errors, it'll just keep reporting
            // over and over... probably a good thing because we'll
            // definitely need to fix it!
        }
    }

    private int findOldestStar() throws Exception {
        String sql = "SELECT stars.id FROM stars" +
                    " WHERE empire_count > 0" +
                    " ORDER BY last_simulation ASC LIMIT 1";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
}
