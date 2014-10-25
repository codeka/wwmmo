package au.com.codeka.warworlds.server;

import java.net.URI;
import java.net.URL;

import org.eclipse.jetty.server.Server;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.cron.CronJobRegistry;
import au.com.codeka.warworlds.server.ctrl.NameGenerator;
import au.com.codeka.warworlds.server.data.SchemaUpdater;
import au.com.codeka.warworlds.server.handlers.admin.AdminGenericHandler;
import au.com.codeka.warworlds.server.model.DesignManager;

/** Main entry-point for the server. */
public class Runner {
  private static final Log log = new Log("Runner");

  public static void main(String[] args) throws Exception {
    String basePath = System.getProperty(SystemProperties.BASE_PATH);
    if (basePath == null) {
      URL url = AdminGenericHandler.class.getClassLoader().getResource("");
      if (url == null) {
        try {
          URI uri = AdminGenericHandler.class.getProtectionDomain().getCodeSource()
              .getLocation().toURI();
          if (uri != null) {
            url = new URL(uri.getPath());
          }
        } catch (Exception e) {}
      }
      if (url != null) {
        basePath = url.getPath() + "../";
      }

      if (basePath == null) {
        throw new Exception("Could not determine basePath, cannot continue");
      }
    }

    if (!basePath.endsWith("/")) {
      basePath += "/";
    }
    System.setProperty(SystemProperties.BASE_PATH, basePath);

    LogImpl.setup();
    try {
      new SchemaUpdater().verifySchema();
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
    } catch (Exception e) {
      log.error("Exception on main thread, aborting.", e);
    }
  }

  private static void cronMain(String method, String extra) throws Exception {
    CronJob job = CronJobRegistry.getJob(method);
    if (job != null) {
      job.run(extra);
    }
  }

  private static void gameMain() throws Exception {
    EventProcessor.i.ping();

    StarSimulatorThread starSimulatorThread = null;
    if (System.getProperty(SystemProperties.DISABLE_STAR_SIMULATION_THREAD) == null) {
      starSimulatorThread = new StarSimulatorThread();
      starSimulatorThread.start();
    } else {
      log.info("Star simulation thread disabled.");
    }

    int port = 8080;
    String portName = System.getProperty(SystemProperties.LISTEN_PORT);
    if (portName != null) {
      port = Integer.parseInt(portName);
    }

    Server server = new Server(port);
    server.setHandler(new RequestRouter());
    server.start();
    log.info("Server started on http://localhost:%d/", port);
    server.join();

    if (starSimulatorThread != null) {
      starSimulatorThread.stop();
    }
  }
}
