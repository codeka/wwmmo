package au.com.codeka.warworlds.client.game.welcome

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.TransparentWebView
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator.setBackground
import au.com.codeka.warworlds.common.proto.Empire
import com.google.common.base.Preconditions
import com.squareup.picasso.Picasso

/**
 * Layout for the [WelcomeScreen].
 */
class WelcomeLayout(context: Context?, private val callbacks: Callbacks)
  : RelativeLayout(context, null) {
  interface Callbacks {
    fun onStartClick()
    fun onHelpClick()
    fun onWebsiteClick()
    fun onSignInClick()
  }

  private val startButton: Button
  private val signInButton: Button
  private val connectionStatus: TextView
  private val empireName: TextView
  private val empireIcon: ImageView
  private val motdView: TransparentWebView

  fun refreshEmpireDetails(empire: Empire) {
    empireName.text = empire.display_name
    Picasso.get()
        .load(ImageHelper.getEmpireImageUrl(context, empire, 20, 20))
        .into(empireIcon)
  }

  fun updateWelcomeMessage(html: String) {
    motdView.loadHtml("html/skeleton.html", html)
  }

  fun setConnectionStatus(connected: Boolean, message: String) {
    startButton.isEnabled = connected
    connectionStatus.text = message
  }

  fun setSignInText(resId: Int) {
    signInButton.setText(resId)
  }

  init {
    View.inflate(context, R.layout.welcome, this)
    setBackground(this)
    startButton = Preconditions.checkNotNull(findViewById(R.id.start_btn))
    signInButton = findViewById(R.id.signin_btn)
    motdView = Preconditions.checkNotNull(findViewById(R.id.motd))
    empireName = Preconditions.checkNotNull(findViewById(R.id.empire_name))
    empireIcon = Preconditions.checkNotNull(findViewById(R.id.empire_icon))
    connectionStatus = Preconditions.checkNotNull(findViewById(R.id.connection_status))
 //   val optionsButton = Preconditions.checkNotNull(findViewById<Button>(R.id.options_btn))
    startButton.setOnClickListener { callbacks.onStartClick() }
    findViewById<View>(R.id.help_btn).setOnClickListener { callbacks.onHelpClick() }
    findViewById<View>(R.id.website_btn).setOnClickListener { callbacks.onWebsiteClick() }
    signInButton.setOnClickListener { callbacks.onSignInClick() }
  }
}
