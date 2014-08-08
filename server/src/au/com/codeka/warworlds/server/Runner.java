package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.cron.CronJobRegistry;
import au.com.codeka.warworlds.server.ctrl.NameGenerator;
import au.com.codeka.warworlds.server.handlers.admin.AdminGenericHandler;
import au.com.codeka.warworlds.server.model.DesignManager;

public class Runner {
    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws Exception {
        String basePath = System.getProperty("au.com.codeka.warworlds.server.basePath");
        if (basePath == null) {
            basePath = AdminGenericHandler.class.getClassLoader().getResource("").getPath();
        }

        LogImpl.setup();
        ErrorReportingLoggingHandler.setup();
        DesignManager.setup(basePath);
        NameGenerator.setup(basePath);

        if (args.length >= 2 && args[0].equals("cron")) {
            String extra = null;
            if (args.length >= 3) {
                extra = args[2];
            }
            cronMain(args[1], extra);
        } else {
            gameMain();
        }
    }

    private static void cronMain(String method, String extra) {
        try {
            CronJob job = CronJobRegistry.getJob(method);
            if (job != null) {
                job.run(extra);
            }
        } catch(Exception e) {
            log.error("Error running CRON", e);
        }
    }

    private static void gameMain() throws Exception {
        EventProcessor.i.ping();

        StarSimulatorThread starSimulatorThread = null;
        if (System.getProperty("au.com.codeka.warworlds.server.disableStarSimulationThread") == null) {
            starSimulatorThread = new StarSimulatorThread();
            starSimulatorThread.start();
        } else {
            log.info("Star simulation thread disabled.");
        }

        int port = 8080;
        String portName = System.getProperty("au.com.codeka.warworlds.server.listenPort");
        if (portName != null) {
            port = Integer.parseInt(portName);
        }

        Server server = new Server(port);
        server.setHandler(new RequestRouter());
        server.start();
        server.join();

        if (starSimulatorThread != null) {
            starSimulatorThread.stop();
        }
    }
}
