package au.com.codeka.warworlds.client.game.welcome

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.SharedViews
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.util.GameSettings
import au.com.codeka.warworlds.client.util.GameSettings.edit

/**
 * This fragment is shown the first time you start the game. We give you a quick intro, some links
 * to the website and stuff like that.
 */
class WarmWelcomeScreen : Screen() {
  private var layout: WarmWelcomeLayout? = null
  override fun onCreate(context: ScreenContext?, container: ViewGroup?) {
    super.onCreate(context, container)
    layout = WarmWelcomeLayout(context!!.activity, object : WarmWelcomeLayout.Callbacks {
      override fun onStartClick() {
        // save the fact that we've finished the warm welcome
        edit()
            .setBoolean(GameSettings.Key.WARM_WELCOME_SEEN, true)
            .commit()
        context.pushScreen(
            CreateEmpireScreen(),
            SharedViews.builder()
                .addSharedView(R.id.title_icon)
                .addSharedView(R.id.title)
                .addSharedView(R.id.next_btn)
                .build())
      }

      override fun onPrivacyPolicyClick() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("http://www.war-worlds.com/privacy-policy")
        context.startActivity(i)
      }

      override fun onHelpClick() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("http://www.war-worlds.com/doc/getting-started")
        context.startActivity(i)
      }
    })
  }

  override fun onShow(): ShowInfo? {
    return ShowInfo.builder().view(layout).toolbarVisible(false).build()
  }
}