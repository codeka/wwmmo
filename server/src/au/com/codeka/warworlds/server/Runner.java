package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Server;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.cron.CronRunnerThread;
import au.com.codeka.warworlds.server.ctrl.NameGenerator;
import au.com.codeka.warworlds.server.data.SchemaUpdater;
import au.com.codeka.warworlds.server.model.DesignManager;

/** Main entry-point for the server. */
public class Runner {
  private static final Log log = new Log("Runner");

  public static void main(String[] args) throws Exception {
    Configuration.loadConfig();
    LogImpl.setup();
    try {
      new SchemaUpdater().verifySchema();
      ErrorReportingLoggingHandler.setup();
      DesignManager.setup();
      NameGenerator.setup();

      EventProcessor.i.ping();
      StarSimulatorThreadManager.i.start();

      CronRunnerThread.setup();

      int port = Configuration.i.getListenPort();
      Server server = new Server(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
      server.setHandler(new RequestRouter());
      server.start();
      log.info("Server started on http://localhost:%d/", port);
      server.join();

      StarSimulatorThreadManager.i.stop();

    } catch (Exception e) {
      log.error("Exception on main thread, aborting.", e);
    }
  }
}
