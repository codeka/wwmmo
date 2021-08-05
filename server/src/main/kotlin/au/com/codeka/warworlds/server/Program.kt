package au.com.codeka.warworlds.server

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Designs
import au.com.codeka.warworlds.common.sim.DesignDefinitions
import au.com.codeka.warworlds.server.admin.AdminServlet
import au.com.codeka.warworlds.server.html.HtmlServlet
import au.com.codeka.warworlds.server.net.ServerSocketManager
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.SmtpHelper
import au.com.codeka.warworlds.server.world.NotificationManager
import au.com.codeka.warworlds.server.world.StarSimulatorQueue
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.gson.GsonBuilder
import com.squareup.wire.WireTypeAdapterFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.io.File

object Program {
  private val log = Log("Runner")

  @JvmStatic
  fun main(args: Array<String>) {
    LogImpl.setup()
    Configuration.i.load()
    DataStore.i.open()
    DesignDefinitions.init(loadDesignDefinitions())
    StarSimulatorQueue.i.start()
    ServerSocketManager.i.start()
    SmtpHelper.i.start()
    NotificationManager.i.start()

    val options = FirebaseOptions.Builder()
        .setCredentials(Configuration.i.getFirebaseCredentials())
        .setDatabaseUrl("https://wwmmo-2.firebaseio.com")
        .build()
    FirebaseApp.initializeApp(options)

    log.info("FirebaseApp initialized.")
    try {
      val port: Int = Configuration.i.listenPort
      val server = Server()
      val connector = ServerConnector(server)
      connector.port = port
      connector.host = "0.0.0.0"
      server.addConnector(connector)
      val context = ServletContextHandler(ServletContextHandler.SESSIONS)
      context.contextPath = "/"
      server.handler = context
      context.addServlet(ServletHolder(AdminServlet::class.java), "/admin/*")
      context.addServlet(ServletHolder(HtmlServlet::class.java), "/*")

      Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
          shutdown(server)
        }
      })

      server.start()
      log.info("Server started on http://localhost:%d/", port)
      server.join()
    } catch (e: Exception) {
      log.error("Exception on main thread, aborting.", e)
    }
  }

  private fun shutdown(server: Server) {
    log.info("Shutdown initiated.")

    try {
      server.stop()
    } catch (e: Exception) {
      log.error("Error stopping HTTP server.", e)
    }

    try {
      ServerSocketManager.i.stop()
    } catch (e: Exception) {
      log.error("Error shutting down server socket manager.", e)
    }

    try {
      StarSimulatorQueue.i.stop()
    } catch (e: Exception) {
      log.error("Error shutting down star simulation queue.", e)
    }

    try {
      SmtpHelper.i.stop()
    } catch (e: Exception) {
      log.error("Error shutting down SMTP helper.", e)
    }

    try {
      DataStore.i.close()
    } catch (e: Exception) {
      log.error("Error shutting down data store.", e)
    }
  }

  private fun loadDesignDefinitions(): Designs {
    val json = File("data/designs/designs.json").readText(Charsets.UTF_8)
    val gson = GsonBuilder()
        .registerTypeAdapterFactory(WireTypeAdapterFactory())
        .create()
    val adapter = gson.getAdapter(Designs::class.java)
    return adapter.fromJson(json)
  }
}
