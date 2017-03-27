package au.com.codeka.warworlds.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.account.AccountAssociateServlet;
import au.com.codeka.warworlds.server.account.AccountVerifyServlet;
import au.com.codeka.warworlds.server.account.AccountsServlet;
import au.com.codeka.warworlds.server.account.LoginServlet;
import au.com.codeka.warworlds.server.admin.AdminServlet;
import au.com.codeka.warworlds.server.net.ServerSocketManager;
import au.com.codeka.warworlds.server.render.RendererServlet;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.util.SmtpHelper;
import au.com.codeka.warworlds.server.world.StarSimulatorQueue;

public class Program {
  private static final Log log = new Log("Runner");

  public static void main(String[] args) throws Exception {
    LogImpl.setup();
    Configuration.i.load();
    DataStore.i.open();
    StarSimulatorQueue.i.start();
    ServerSocketManager.i.start();
    SmtpHelper.i.start();

    try {
      int port = Configuration.i.getListenPort();

      Server server = new Server();
      ServerConnector connector = new ServerConnector(server);
      connector.setPort(port);
      server.addConnector(connector);

      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);

      context.addServlet(new ServletHolder(AccountsServlet.class), "/accounts");
      context.addServlet(new ServletHolder(AccountAssociateServlet.class), "/accounts/associate");
      context.addServlet(new ServletHolder(AccountVerifyServlet.class), "/accounts/verify");
      context.addServlet(new ServletHolder(LoginServlet.class), "/login");
      context.addServlet(new ServletHolder(RendererServlet.class), "/render/*");
      context.addServlet(new ServletHolder(AdminServlet.class), "/admin/*");

      server.start();
      log.info("Server started on http://localhost:%d/", port);
      server.join();

    } catch (Exception e) {
      log.error("Exception on main thread, aborting.", e);
    } finally {
      log.info("Shutting down.");
      ServerSocketManager.i.stop();
      StarSimulatorQueue.i.stop();
      SmtpHelper.i.stop();
      DataStore.i.close();
    }
  }
}
