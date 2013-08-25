package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.ctrl.CombatReportController;
import au.com.codeka.warworlds.server.ctrl.NameGenerator;
import au.com.codeka.warworlds.server.ctrl.SessionController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.StatisticsController;
import au.com.codeka.warworlds.server.handlers.pages.HtmlPageHandler;
import au.com.codeka.warworlds.server.model.DesignManager;

public class Runner {
    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws Exception {
        String basePath = System.getProperty("au.com.codeka.warworlds.server.basePath");
        if (basePath == null) {
            basePath = HtmlPageHandler.class.getClassLoader().getResource("").getPath();
        }

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
            if (method.equals("update-ranks")) {
                new StatisticsController().updateRanks();
            } else if (method.equals("simulate-stars")) {
                DateTime dt = DateTime.now().minusHours(extraToNum(extra, 0, 6));
                new StarController().simulateAllStarsOlderThan(dt);
            } else if (method.equals("purge-combat-reports")) {
                DateTime dt = DateTime.now().minusDays(extraToNum(extra, 7, 30));
                new CombatReportController().purgeCombatReportsOlderThan(dt);
            } else if (method.equals("purge-sessions")) {
                DateTime dt = DateTime.now().minusDays(extraToNum(extra, 1, 7));
                new SessionController().purgeSessionsOlderThan(dt);
            } else {
                log.error("Unknown command: "+method);
            }
        } catch(Exception e) {
            log.error("Error running CRON", e);
        }
    }

    private static int extraToNum(String extra, int minNumber, int defaultNumber) {
        int num = defaultNumber;
        if (extra != null) {
            num = Integer.parseInt(extra);
        }
        if (num < minNumber) {
            num = minNumber;
        }
        return num;
    }

    private static void gameMain() throws Exception {
        EventProcessor.i.ping();

        int port = 8080;
        String portName = System.getProperty("au.com.codeka.warworlds.server.listenPort");
        if (portName != null) {
            port = Integer.parseInt(portName);
        }

        Server server = new Server(port);
        server.setHandler(new RequestRouter());
        server.start();
        server.join();
    }
}
