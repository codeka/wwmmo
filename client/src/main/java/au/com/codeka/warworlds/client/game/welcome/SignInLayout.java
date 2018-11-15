package au.com.codeka.warworlds.client.game.welcome;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Layout for {@link SignInScreen}.
 */
public class SignInLayout extends RelativeLayout {
  interface Callbacks {
    void onSignInClick(String emailAddr);
    void onCancelClick();
  }

  private final Button signInButton;
  private final Button cancelButton;
  private final EditText emailText;
  private final TextView signInHelp;
  private final TextView signInError;

  public SignInLayout(Context context, Callbacks callbacks) {
    super(context);
    inflate(context, R.layout.signin, this);
    ViewBackgroundGenerator.setBackground(this);

    signInHelp = checkNotNull(findViewById(R.id.signin_help));
    signInError = checkNotNull(findViewById(R.id.signin_error));
    signInButton = checkNotNull(findViewById(R.id.signin_btn));
    cancelButton = checkNotNull(findViewById(R.id.cancel_btn));
    emailText = checkNotNull(findViewById(R.id.email));
    if (GameSettings.i.getString(GameSettings.Key.EMAIL_ADDR).isEmpty()) {
      signInButton.setText(R.string.next);
    } else {
      signInButton.setText(R.string.switch_user);
    }
    signInButton.setOnClickListener(v -> callbacks.onSignInClick(emailText.getText().toString()));

    cancelButton.setOnClickListener(v -> callbacks.onCancelClick());
  }

  /**
   * Update the state of the UI.
   *
   * @param helpResId The resource ID to use for the help text.
   * @param signInResId
   * @param isCancel
   * @param showEmailAddr
   */
  public void updateState(
      int helpResId,
      boolean signInEnabled,
      int signInResId,
      boolean isCancel,
      boolean showEmailAddr) {
    if (showEmailAddr) {
      emailText.requestFocus();
      emailText.setVisibility(View.VISIBLE);
    } else {
      emailText.setVisibility(View.GONE);
    }
    signInHelp.setText(helpResId);
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(emailText, InputMethodManager.SHOW_IMPLICIT);
    signInButton.setEnabled(signInEnabled);
    signInButton.setText(signInResId);
    cancelButton.setText(isCancel ? R.string.cancel : R.string.back);
  }

  public void hideKeyboard() {
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.hideSoftInputFromWindow(emailText.getWindowToken(), 0);
    }
  }

  public void showKeyboard() {

  }

  public void setForceSignIn(int helpResId) {
    signInHelp.setText(helpResId);
    signInButton.setText(R.string.yes);
    cancelButton.setText(R.string.no);
  }

  public void setErrorMsg(int resId) {
    signInError.setText(resId);
  }
  public void setErrorMsg(String errorMsg) {
    if (errorMsg == null) {
      signInError.setText("");
    } else {
      signInError.setText(errorMsg);
    }
  }
}
