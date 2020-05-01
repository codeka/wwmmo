package au.com.codeka.warworlds.client.game.welcome

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.util.GameSettings
import au.com.codeka.warworlds.client.util.GameSettings.getString
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator.setBackground
import com.google.common.base.Preconditions

/**
 * Layout for [SignInScreen].
 */
class SignInLayout(context: Context?, callbacks: Callbacks) : RelativeLayout(context) {
  interface Callbacks {
    fun onSignInClick()
    fun onCancelClick()
  }

  private val signInButton: Button
  private val cancelButton: Button
  private val signInHelp: TextView
  private val signInError: TextView

  init {
    View.inflate(context, R.layout.signin, this)
    setBackground(this)
    signInHelp = Preconditions.checkNotNull(findViewById(R.id.signin_help))
    signInError = Preconditions.checkNotNull(findViewById(R.id.signin_error))
    signInButton = Preconditions.checkNotNull(findViewById(R.id.signin_btn))
    cancelButton = Preconditions.checkNotNull(findViewById(R.id.cancel_btn))
    if (getString(GameSettings.Key.EMAIL_ADDR).isEmpty()) {
      signInButton.setText(R.string.next)
    } else {
      signInButton.setText(R.string.switch_user)
    }
    signInButton.setOnClickListener { v: View? -> callbacks.onSignInClick() }
    cancelButton.setOnClickListener { v: View? -> callbacks.onCancelClick() }
  }

  /**
   * Update the state of the UI.
   *
   * @param helpResId The resource ID to use for the help text.
   * @param signInResId
   * @param isCancel
   */
  fun updateState(
      helpResId: Int,
      signInEnabled: Boolean,
      signInResId: Int,
      isCancel: Boolean) {
    signInHelp.setText(helpResId)

    signInButton.isEnabled = signInEnabled
    signInButton.setText(signInResId)
    cancelButton.setText(if (isCancel) R.string.cancel else R.string.back)
  }

  fun setForceSignIn(helpResId: Int) {
    signInHelp.setText(helpResId)
    signInButton.setText(R.string.yes)
    cancelButton.setText(R.string.no)
  }

  fun setErrorMsg(resId: Int) {
    signInError.setText(resId)
  }

  fun setErrorMsg(errorMsg: String?) {
    if (errorMsg == null) {
      signInError.text = ""
    } else {
      signInError.text = errorMsg
    }
  }
}