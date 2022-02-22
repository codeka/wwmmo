package au.com.codeka.warworlds.client.game.empire

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.empire.SettingsView.PatreonConnectCompleteCallback
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.net.HttpRequest
import au.com.codeka.warworlds.client.net.ServerUrl.getUrl
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.ui.ShowInfo.Companion.builder
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.PatreonBeginRequest
import au.com.codeka.warworlds.common.proto.PatreonBeginResponse

/**
 * This screen shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
class EmpireScreen : Screen() {
  private lateinit var context: ScreenContext
  private lateinit var layout: EmpireLayout

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    this.context = context
    layout = EmpireLayout(context.activity, SettingsCallbacks())
  }

  override fun onShow(): ShowInfo {
    return builder().view(layout).build()
  }

  private inner class SettingsCallbacks : SettingsView.Callback {
    override fun onPatreonConnectClick(
        completeCallback: PatreonConnectCompleteCallback) {
      App.taskRunner.runTask(Runnable {
        val req = HttpRequest.Builder()
            .url(getUrl("/accounts/patreon-begin"))
            .authenticated()
            .body(PatreonBeginRequest(empire_id = EmpireManager.getMyEmpire().id).encode())
            .method(HttpRequest.Method.POST)
            .build()
        if (req.responseCode != 200 || req.exception != null) {
          // TODO: better error handling.
          log.error("Error starting patreon connect request: %d %s",
              req.responseCode, req.exception)
          completeCallback.onPatreonConnectComplete("Unexpected error.")
          return@Runnable
        }
        val resp = req.getBody(PatreonBeginResponse::class.java)
        if (resp == null) {
          // TODO: better error handling.
          log.error("Got an empty response?")
          completeCallback.onPatreonConnectComplete("Unexpected error.")
          return@Runnable
        }
        val uri = ("https://www.patreon.com/oauth2/authorize?response_type=code"
            + "&client_id=" + resp.client_id
            + "&redirect_uri=" + Uri.encode(resp.redirect_uri)
            + "&state=" + Uri.encode(resp.state))
        log.info("Opening URL: %s", uri)
        App.taskRunner.runTask({
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
          context.activity.startActivity(intent)
        }, Threads.UI)
      }, Threads.BACKGROUND)
    }
  }

  companion object {
    private val log = Log("EmpireScreen")
  }
}