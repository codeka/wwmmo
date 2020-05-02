package au.com.codeka.warworlds.client.game.welcome

import android.view.ViewGroup
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.net.HttpRequest
import au.com.codeka.warworlds.client.net.ServerUrl.url
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.SharedViews
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.util.GameSettings
import au.com.codeka.warworlds.client.util.GameSettings.edit
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.NewAccountRequest
import au.com.codeka.warworlds.common.proto.NewAccountResponse

/**
 * This screen is shown when you don't have a cookie saved. We'll want to either let you create
 * a new empire, or sign in with an existing account (if you have one).
 */
class CreateEmpireScreen : Screen() {
  private lateinit var layout: CreateEmpireLayout
  private lateinit var context: ScreenContext

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)

    this.context = context
    layout = CreateEmpireLayout(context.activity, layoutCallbacks)
  }

  override fun onShow(): ShowInfo? {
    return ShowInfo.builder().view(layout).toolbarVisible(false).build()
  }

  private val layoutCallbacks = object : CreateEmpireLayout.Callbacks {
    override fun onDoneClick(empireName: String?) {
      registerEmpire(empireName!!)
    }

    override fun onSwitchAccountClick() {
      context.pushScreen(SignInScreen())
    }
  }

  private fun registerEmpire(empireName: String) {
    layout.showSpinner()
    App.taskRunner.runTask(Runnable {
      val request = HttpRequest.Builder()
          .url(url + "accounts")
          .method(HttpRequest.Method.POST)
          .header("Content-Type", "application/x-protobuf")
          .body(NewAccountRequest.Builder()
              .empire_name(empireName)
              .build().encode())
          .build()
      val resp = request.getBody(NewAccountResponse::class.java)
      if (resp == null) {
        // TODO: report the error to the server?
        log.error("Didn't get NewAccountResponse, as expected.", request.exception)
      } else if (resp.cookie == null) {
        App.taskRunner.runTask(Runnable { layout.showError(resp.message) }, Threads.UI)
      } else {
        log.info(
            "New account response, cookie: %s, message: %s",
            resp.cookie,
            resp.message)
        App.taskRunner.runTask(Runnable { onRegisterSuccess(resp) }, Threads.UI)
      }
    }, Threads.BACKGROUND)
  }

  private fun onRegisterSuccess(resp: NewAccountResponse) {
    // Save the cookie.
    edit()
        .setString(GameSettings.Key.COOKIE, resp.cookie)
        .commit()

    // Tell the Server we can now connect.
    App.server.connect()
    context.pushScreen(
        WelcomeScreen(),
        SharedViews.builder()
            .addSharedView(R.id.next_btn, R.id.start_btn)
            .addSharedView(R.id.title)
            .addSharedView(R.id.title_icon)
            .build())
  }

  companion object {
    private val log = Log("CreateEmpireScreen")
  }
}