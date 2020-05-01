package au.com.codeka.warworlds.client

import android.app.Application
import android.os.Handler
import au.com.codeka.warworlds.client.concurrency.TaskRunner
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.world.ChatManager
import au.com.codeka.warworlds.client.game.world.StarManager.create
import au.com.codeka.warworlds.client.net.Server
import au.com.codeka.warworlds.client.net.auth.AuthHelper
import au.com.codeka.warworlds.client.store.DataStore
import au.com.codeka.warworlds.client.util.eventbus.EventBus
import au.com.codeka.warworlds.common.Log
import com.squareup.picasso.Picasso

/**
 * Global [Application] object.
 */
class MyApp : Application() {
  val taskRunner: TaskRunner
  val server: Server
  val dataStore: DataStore
  val eventBus: EventBus
  val auth: AuthHelper

  override fun onCreate() {
    super.onCreate()
    LogImpl.setup()
    Picasso.setSingletonInstance(
        Picasso.Builder(this)
            .loggingEnabled(true)
            .build())
    Threads.UI.setThread(Thread.currentThread(), Handler())
    server.connect()
    dataStore.open(this)
    create()
    ChatManager.i.create()
    log.info("App.onCreate() complete.")
  }

  companion object {
    private val log = Log("App")
  }

  init {
    App = this
    taskRunner = TaskRunner()
    server = Server()
    dataStore = DataStore()
    eventBus = EventBus()
    auth = AuthHelper(this)
  }
}

lateinit var App : MyApp