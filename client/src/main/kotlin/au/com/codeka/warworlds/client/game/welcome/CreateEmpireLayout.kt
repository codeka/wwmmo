package au.com.codeka.warworlds.client.game.welcome

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator.setBackground

/**
 * Layout for [CreateEmpireScreen].
 */
class CreateEmpireLayout(context: Context?, private val callbacks: Callbacks) : RelativeLayout(context) {
  interface Callbacks {
    fun onDoneClick(empireName: String?)
  }

  private val empireName: EditText
  fun showSpinner() {
    findViewById<View>(R.id.empire_name).visibility = View.GONE
    findViewById<View>(R.id.switch_account_btn).visibility = View.GONE
    findViewById<View>(R.id.progress).visibility = View.VISIBLE
  }

  /** Hide the spinner again (so the user can try again) but show an error message as well.  */
  fun showError(msg: String?) {
    findViewById<View>(R.id.empire_name).visibility = View.VISIBLE
    findViewById<View>(R.id.switch_account_btn).visibility = View.VISIBLE
    findViewById<View>(R.id.progress).visibility = View.GONE
    (findViewById<View>(R.id.setup_name) as TextView).text = msg
  }

  init {
    View.inflate(context, R.layout.create_empire, this)
    empireName = findViewById(R.id.empire_name)
    setBackground(this)
    findViewById<View>(R.id.next_btn).setOnClickListener { v: View? -> callbacks.onDoneClick(empireName.text.toString()) }
  }
}