package au.com.codeka.warworlds.server;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.FileInputStream;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.account.AccountAssociateServlet;
import au.com.codeka.warworlds.server.account.AccountsServlet;
import au.com.codeka.warworlds.server.account.LoginServlet;
import au.com.codeka.warworlds.server.admin.AdminServlet;
import au.com.codeka.warworlds.server.net.ServerSocketManager;
import au.com.codeka.warworlds.server.render.RendererServlet;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.StarSimulatorQueue;

public class Program {
  private static final Log log = new Log("Runner");

  public static void main(String[] args) throws Exception {
    //Configuration.loadConfig();
    LogImpl.setup();
    DataStore.i.open();

    try {
      FileInputStream firebaseCertificate = new FileInputStream("data/firebase.json");
      FirebaseOptions options = new FirebaseOptions.Builder()
          .setCredential(FirebaseCredentials.fromCertificate(firebaseCertificate))
          .setDatabaseUrl("https://wwmmo-93bac.firebaseio.com")
          .build();
      FirebaseApp.initializeApp(options);

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
      log.info("Shutting down.");
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
    StarSimulatorQueue.i.start();
    ServerSocketManager.i.start();

    try {
      int port = 8080;//Configuration.i.getListenPort();

      Server server = new Server();
      ServerConnector connector = new ServerConnector(server);
      connector.setPort(port);
      server.addConnector(connector);

      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);

      context.addServlet(new ServletHolder(AccountsServlet.class), "/accounts");
      context.addServlet(new ServletHolder(AccountAssociateServlet.class), "/accounts/associate");
      context.addServlet(new ServletHolder(LoginServlet.class), "/login");
      context.addServlet(new ServletHolder(RendererServlet.class), "/render/*");
      context.addServlet(new ServletHolder(AdminServlet.class), "/admin/*");

      server.start();
      log.info("Server started on http://localhost:%d/", port);
      server.join();
    } finally {
      ServerSocketManager.i.stop();
      StarSimulatorQueue.i.stop();
    }
  }
}
