package au.com.codeka.warworlds.client.game.welcome;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.ctrl.TransparentWebView;
import au.com.codeka.warworlds.client.game.starfield.StarfieldFragment;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.common.Log;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This fragment is used to associate your cookie with an email address. You'll get a code in your
 * email account that you then have to type into the box.
 */
public class SignInFragment extends BaseFragment {
  private static final Log log = new Log("SignInFragment");

  private Button signInButton;
  private Button cancelButton;
  private EditText emailText;

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_signin;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ViewBackgroundGenerator.setBackground(view);

    signInButton = (Button) checkNotNull(view.findViewById(R.id.signin_btn));
    cancelButton = (Button) checkNotNull(view.findViewById(R.id.cancel_btn));
    emailText = (EditText) checkNotNull(view.findViewById(R.id.email));
    if (GameSettings.i.getString(GameSettings.Key.EMAIL_ADDR).isEmpty()) {
      signInButton.setTag(R.string.signin);
    } else {
      signInButton.setText(R.string.switch_user);
    }
//    signInButton.setOnClickListener(v -> onSignInClick());


    cancelButton.setOnClickListener(v -> {
      InputMethodManager imm =
          (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(emailText.getWindowToken(), 0);
      getFragmentManager().popBackStack();
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    emailText.requestFocus();
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(emailText, InputMethodManager.SHOW_IMPLICIT);
  }

}
