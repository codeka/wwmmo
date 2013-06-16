package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.ctrl.NameGenerator;
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
                int numHours = 6;
                if (extra != null) {
                    numHours = Integer.parseInt(extra);
                }
                DateTime dt = DateTime.now();
                if (numHours > 0) {
                    dt = dt.minusHours(numHours);
                }
                new StarController().simulateAllStarsOlderThan(dt);
            }
        } catch(Exception e) {
            log.error("Error running CRON", e);
        }
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
