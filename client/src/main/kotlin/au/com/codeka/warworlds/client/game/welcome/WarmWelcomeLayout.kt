package au.com.codeka.warworlds.client.game.welcome

import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.TransparentWebView
import au.com.codeka.warworlds.client.ctrl.TransparentWebView.Companion.getHtmlFile
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator.setBackground

/**
 * Layout for [WarmWelcomeScreen].
 */
class WarmWelcomeLayout(context: Context?, callbacks: Callbacks) : RelativeLayout(context) {
  interface Callbacks {
    fun onStartClick()
    fun onPrivacyPolicyClick()
    fun onHelpClick()
  }

  private val callbacks: Callbacks

  init {
    View.inflate(context, R.layout.warm_welcome, this)
    this.callbacks = callbacks
    setBackground(this)
    val welcome: TransparentWebView = findViewById(R.id.welcome)
    val msg = getHtmlFile(getContext(), "html/warm-welcome.html")
    welcome.loadHtml("html/skeleton.html", msg)
    findViewById<View>(R.id.next_btn).setOnClickListener { callbacks.onStartClick() }
    findViewById<View>(R.id.help_btn).setOnClickListener { callbacks.onHelpClick() }
    findViewById<View>(R.id.privacy_policy_btn).setOnClickListener {
        callbacks.onPrivacyPolicyClick() }
  }
}