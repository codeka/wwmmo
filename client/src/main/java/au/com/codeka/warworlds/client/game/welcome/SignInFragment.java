package au.com.codeka.warworlds.client.game.welcome;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.net.HttpRequest;
import au.com.codeka.warworlds.client.net.ServerUrl;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AccountAssociateRequest;
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse;
import au.com.codeka.warworlds.common.proto.Empire;

/**
 * This fragment is used to associate your cookie with an email address. You'll get a code in your
 * email account that you then have to type into the box.
 */
public class SignInFragment extends BaseFragment {
  private static final Log log = new Log("SignInFragment");

  private Button signInButton;
  private Button cancelButton;
  private EditText emailText;
  private TextView signInHelp;
  private TextView signInError;

  /** If we're tried to associate and got an error that can be fixed by forcing, this'll be true. */
  private boolean askingToForce = false;

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_signin;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ViewBackgroundGenerator.setBackground(view);

    signInHelp = checkNotNull(view.findViewById(R.id.signin_help));
    signInError = checkNotNull(view.findViewById(R.id.signin_error));
    signInButton = checkNotNull(view.findViewById(R.id.signin_btn));
    cancelButton = checkNotNull(view.findViewById(R.id.cancel_btn));
    emailText = checkNotNull(view.findViewById(R.id.email));
    if (GameSettings.i.getString(GameSettings.Key.EMAIL_ADDR).isEmpty()) {
      signInButton.setText(R.string.next);
    } else {
      signInButton.setText(R.string.switch_user);
    }
    signInButton.setOnClickListener(v -> onSignInClick());

    cancelButton.setOnClickListener(v -> onCancelClick());
  }

  @Override
  public void onResume() {
    super.onResume();
    updateState(null);

    GameSettings.SignInState signInState =
        GameSettings.i.getEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.class);
    if (signInState == GameSettings.SignInState.AWAITING_VERIFICATION) {
      checkVerficationStatus();
    }
  }

  /** Updates the current state (or just refreshes the current state, if newState is null). */
  private void updateState(@Nullable GameSettings.SignInState newState) {
    GameSettings.SignInState signInState;
    if (newState != null) {
      GameSettings.i.edit()
          .setEnum(GameSettings.Key.SIGN_IN_STATE, newState)
          .commit();
      signInState = newState;
    } else {
      signInState =
          GameSettings.i.getEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.class);
    }

    signInError.setText("");

    switch (signInState) {
      case ANONYMOUS:
        emailText.requestFocus();
        InputMethodManager imm =
            (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(emailText, InputMethodManager.SHOW_IMPLICIT);
        signInButton.setEnabled(true);
        cancelButton.setText(R.string.cancel);
        break;
      case AWAITING_VERIFICATION:
        signInHelp.setText(R.string.signin_code_help);
        signInButton.setEnabled(false);
        cancelButton.setText(R.string.cancel);
        emailText.setVisibility(View.GONE);
        break;
      case VERIFIED:
        signInHelp.setText(R.string.signin_complete_help);
        signInButton.setEnabled(true);
        signInButton.setText(R.string.switch_user);
        cancelButton.setText(R.string.back);

        emailText.setVisibility(View.GONE);
        break;
      default:
        throw new IllegalStateException("Unexpected sign-in state: " + signInState);
    }
  }

  private void onSignInClick() {
    if (askingToForce) {
      onSignInForce();
      return;
    }

    GameSettings.SignInState signInState =
        GameSettings.i.getEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.class);
    switch(signInState) {
      case ANONYMOUS:
        onSignInAnonymousState();
        break;
      case AWAITING_VERIFICATION:
        // Shouldn't happen.
        break;
      case VERIFIED:
        onSignInVerifiedState();
        break;
      default:
        throw new IllegalStateException("Unexpected sign-in state: " + signInState);
    }
  }

  private void onCancelClick() {
    GameSettings.SignInState signInState =
        GameSettings.i.getEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.class);
    if (signInState != GameSettings.SignInState.VERIFIED) {
      // if you're not yet verified, go back to the anonymous state.
      GameSettings.i.edit()
          .setEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.ANONYMOUS)
          .commit();
    }

    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(emailText.getWindowToken(), 0);

    getFragmentManager().popBackStack();
  }

  private void onSignInAnonymousState() {
    doAssociate(false);
  }

  private void onSignInForce() {
    doAssociate(true);
  }

  private void doAssociate(boolean force) {
    String emailAddr = emailText.getText().toString();
    Empire myEmpire = EmpireManager.i.getMyEmpire();
    log.info("Associating email address '%s' with empire #%d %s (force=%s)...",
        emailAddr, myEmpire.id, myEmpire.display_name, force ? "true" : "false");

    signInHelp.setText(R.string.signin_code_help);
    emailText.setVisibility(View.GONE);
    signInButton.setEnabled(false);

    App.i.getTaskRunner().runTask(() -> {
      HttpRequest request = new HttpRequest.Builder()
          .url(ServerUrl.getUrl() + "accounts/associate")
          .method(HttpRequest.Method.POST)
          .header("Content-Type", "application/x-protobuf")
          .body(new AccountAssociateRequest.Builder()
              .cookie(GameSettings.i.getString(GameSettings.Key.COOKIE))
              .force(force)
              .email_addr(emailAddr)
              .build().encode())
          .build();
      AccountAssociateResponse resp = request.getBody(AccountAssociateResponse.class);
      if (resp == null) {
        // TODO: report the error
        log.error("Didn't get AccountAssociateResponse, as expected.", request.getException());
        App.i.getTaskRunner().runTask(() ->
                onSignInError(
                    AccountAssociateResponse.AccountAssociateStatus.STATUS_UNKNOWN,
                    "An unknown error occurred."),
            Threads.UI);
      } else if (resp.status != AccountAssociateResponse.AccountAssociateStatus.SUCCESS) {
        App.i.getTaskRunner().runTask(() -> onSignInError(resp.status, null), Threads.UI);
      } else {
        log.info("Associate successful, awaiting verification code");
        App.i.getTaskRunner().runTask(
            () -> {
              signInButton.setEnabled(false);
              updateState(GameSettings.SignInState.AWAITING_VERIFICATION);
            },
            Threads.UI);
      }
    }, Threads.BACKGROUND);
  }

  /** If you're verified, then hitting sign in again is for switching accounts. */
  private void onSignInVerifiedState() {
    // TODO
  }

  private void onSignInError(
      AccountAssociateResponse.AccountAssociateStatus status,
      @Nullable String msg) {
    signInButton.setEnabled(true);
    if (msg == null) {
      switch (status) {
        case ACCOUNT_ALREADY_ASSOCIATED:
          signInHelp.setText(R.string.signin_error_account_already_exists);
          askingToForce = true;
          break;
        case EMAIL_ALREADY_ASSOCIATED:
          signInHelp.setText(R.string.signin_error_email_already_associated);
          askingToForce = true;
          break;
        case UNEXPECTED_ERROR:
        default:
          signInHelp.setText(R.string.signin_error_unknown);
          askingToForce = false;
          break;
      }
    } else {
      signInError.setText(msg);
    }

    if (askingToForce) {
      signInButton.setText(R.string.yes);
      cancelButton.setText(R.string.no);
    } else {
      signInButton.setEnabled(false);
      cancelButton.setText(R.string.cancel);
    }
  }

  private void checkVerficationStatus() {
    Empire myEmpire = EmpireManager.i.getMyEmpire();
    App.i.getTaskRunner().runTask(() -> {
      HttpRequest request = new HttpRequest.Builder()
          .url(ServerUrl.getUrl() + "accounts/associate?id=" + myEmpire.id)
          .method(HttpRequest.Method.GET)
          .build();
      AccountAssociateResponse resp = request.getBody(AccountAssociateResponse.class);
      if (resp == null) {
        // TODO: report the error
        log.error("Didn't get AccountAssociateResponse, as expected.", request.getException());
        App.i.getTaskRunner().runTask(() ->
            onSignInError(
                AccountAssociateResponse.AccountAssociateStatus.STATUS_UNKNOWN,
                "An unknown error occurred."),
            Threads.UI);
      } else if (resp.status != AccountAssociateResponse.AccountAssociateStatus.SUCCESS) {
        // Just wait.
      } else {
        log.info("Associate successful, verification complete.");
        App.i.getTaskRunner().runTask(
            () -> updateState(GameSettings.SignInState.VERIFIED),
            Threads.UI);
      }
    }, Threads.BACKGROUND);
  }
}
