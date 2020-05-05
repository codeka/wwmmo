package au.com.codeka.warworlds.client.game.welcome

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator.setBackground

/** Layout for [CreateEmpireScreen]. */
class CreateEmpireLayout(context: Context?, private val callbacks: Callbacks)
    : RelativeLayout(context) {
  interface Callbacks {
    fun onSwitchAccountClick()
    fun onDoneClick(empireName: String?)
  }

  private val empireName: EditText
  private val switchAccountButton: Button

  init {
    View.inflate(context, R.layout.create_empire, this)
    empireName = findViewById(R.id.empire_name)
    switchAccountButton = findViewById(R.id.switch_account_btn)
    setBackground(this)
    findViewById<View>(R.id.next_btn).setOnClickListener {
      callbacks.onDoneClick(empireName.text.toString())
    }
    findViewById<View>(R.id.switch_account_btn).setOnClickListener {
      callbacks.onSwitchAccountClick()
    }

    // If you're already signed in, no need to switch accounts (we'll associate this empire with
    // the account you're signed in as).
    if (App.auth.isSignedIn) {
      switchAccountButton.visibility = View.GONE
    }
  }

  fun showSpinner() {
    empireName.visibility = View.GONE
    switchAccountButton.visibility = View.GONE
    findViewById<View>(R.id.progress).visibility = View.VISIBLE
  }

  /** Hide the spinner again (so the user can try again) but show an error message as well.  */
  fun showError(msg: String?) {
    empireName.visibility = View.VISIBLE
    switchAccountButton.visibility = View.VISIBLE
    findViewById<View>(R.id.progress).visibility = View.GONE
    (findViewById<View>(R.id.setup_name) as TextView).text = msg
  }

}
