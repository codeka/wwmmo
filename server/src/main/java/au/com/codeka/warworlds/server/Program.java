package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.account.AccountsServlet;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.websock.GameWebSocketServlet;

public class Program {
  private static final Log log = new Log("Runner");

  public static void main(String[] args) throws Exception {
    //Configuration.loadConfig();
    LogImpl.setup();
    try {
      //new SchemaUpdater().verifySchema();
      //ErrorReportingLoggingHandler.setup();
      //DesignManager.setup();
      //NameGenerator.setup();

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
    } finally {
      DataStore.i.close();
    }
  }

  private static void cronMain(String method, String extra) throws Exception {
    //CronJob job = CronJobRegistry.getJob(method);
    //if (job != null) {
    //  job.run(extra);
    //}
  }

  private static void gameMain() throws Exception {
    //EventProcessor.i.ping();

    //StarSimulatorThreadManager starSimulatorThreadManager = new StarSimulatorThreadManager();
    //starSimulatorThreadManager.start();

    int port = 8080;//Configuration.i.getListenPort();

    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(port);
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);

    context.addServlet(new ServletHolder("conn", GameWebSocketServlet.class), "/conn");
    context.addServlet(new ServletHolder("accounts", AccountsServlet.class), "/accounts");

    server.start();
    log.info("Server started on http://localhost:%d/", port);
    server.join();

    //starSimulatorThreadManager.stop();
  }
}
