package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Logger;
import org.slf4j.helpers.MessageFormatter;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.cron.CronRunnerThread;
import au.com.codeka.warworlds.server.ctrl.NameGenerator;
import au.com.codeka.warworlds.server.data.SchemaUpdater;
import au.com.codeka.warworlds.server.metrics.MetricsManager;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.monitor.RequestStatMonitor;

/** Main entry-point for the server. */
public class Runner {
  private static final Log log = new Log("Runner");

  private static int exitCode;
  private static Server server;

  public static void main(String[] args) throws Exception {
    Configuration.loadConfig();
    LogImpl.setup();
    try {
      new SchemaUpdater().verifySchema();
      ErrorReportingLoggingHandler.setup();
      DesignManager.setup();
      NameGenerator.setup();

      MetricsManager.i.start();
      EventProcessor.i.ping();
      StarSimulatorThreadManager.i.start();

      CronRunnerThread.setup();

      Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));

      int port = Configuration.i.getListenPort();
      server = new Server(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
      server.setHandler(new RequestRouter());
      server.start();
      log.info("Server started on http://localhost:%d/", port);
      server.join();

      log.info("Server exiting with status code %d", exitCode);
      System.exit(exitCode);
    } catch (Throwable e) {
      log.error("Exception on main thread, aborting.", e);
    }
  }

  public static void stop(int exitCode) {
    Runner.exitCode = exitCode;

    log.info("Server shutting down.");
    try {
      server.stop();
    } catch(Exception e) {
      log.error("Unexpected error stopping server.", e);
      // If we get an error, we'll just System.exit() forcefully.
      System.exit(exitCode);
    }
  }

  private static final Runnable shutdownHook = () -> {
    try {
      log.info("Stopping star simulator thread");
      StarSimulatorThreadManager.i.stop();

      log.info("Stopping cron thread");
      CronRunnerThread.cleanup();

      log.info("Cleaning up request stat monitor");
      RequestStatMonitor.i.cleanup();

      log.info("Stopping metrics manager.");
      MetricsManager.i.stop();

      log.info("Everything is shut down.");
    } catch (Exception e) {
      log.error("Error caught during shut down", e);
    }
  };
}
