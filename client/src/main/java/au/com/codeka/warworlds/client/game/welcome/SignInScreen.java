package au.com.codeka.warworlds.client.game.welcome;

import android.view.ViewGroup;

import androidx.annotation.Nullable;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.net.HttpRequest;
import au.com.codeka.warworlds.client.net.ServerUrl;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.ShowInfo;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AccountAssociateRequest;
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse;
import au.com.codeka.warworlds.common.proto.Empire;

/**
 * This screen is used to associate your cookie with an email address. You'll get a code in your
 * email account that you then have to type into the box.
 */
public class SignInScreen extends Screen {
  private static final Log log = new Log("SignInScreen");

  private SignInLayout layout;
  private ScreenContext context;

  /** If we're tried to associate and got an error that can be fixed by forcing, this'll be true. */
  private boolean askingToForce = false;

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);
    this.context = context;
    layout = new SignInLayout(context.getActivity(), layoutCallbacks);
  }

  @Override
  public ShowInfo onShow() {
    updateState(null);

    GameSettings.SignInState signInState =
        GameSettings.i.getEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.class);
    if (signInState == GameSettings.SignInState.AWAITING_VERIFICATION) {
      checkVerificationStatus();
    }

    return ShowInfo.builder().view(layout).toolbarVisible(false).build();
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

    layout.setErrorMsg(null);
    switch (signInState) {
      case ANONYMOUS:
        layout.updateState(
            R.string.signin_help,
            true /* signInEnabled */,
            R.string.signin,
            true /* isCancel */,
            true /* showEmailAddr */);
        break;
      case AWAITING_VERIFICATION:
        layout.updateState(
            R.string.signin_code_help,
            false /* signInEnabled */,
            R.string.signin,
            true /* isCancel */,
            false /* showEmailAddr */);
        break;
      case VERIFIED:
        layout.updateState(
            R.string.signin_complete_help,
            true /* signInEnabled */,
            R.string.switch_user,
            false /* isCancel */,
            false /* showEmailAddr */);
        break;
      default:
        throw new IllegalStateException("Unexpected sign-in state: " + signInState);
    }
  }

  private final SignInLayout.Callbacks layoutCallbacks = new SignInLayout.Callbacks() {
    @Override
    public void onSignInClick(String emailAddr) {
      if (askingToForce) {
        onSignInForce(emailAddr);
        return;
      }

      GameSettings.SignInState signInState =
          GameSettings.i.getEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.class);
      switch(signInState) {
        case ANONYMOUS:
          onSignInAnonymousState(emailAddr);
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

    @Override
    public void onCancelClick() {
      GameSettings.SignInState signInState =
          GameSettings.i.getEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.class);
      if (signInState != GameSettings.SignInState.VERIFIED) {
        // if you're not yet verified, go back to the anonymous state.
        GameSettings.i.edit()
            .setEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.ANONYMOUS)
            .commit();
      }

      layout.hideKeyboard();

      //TODO getFragmentManager().popBackStack();
      context.popScreen();
    }
  };

  private void onSignInAnonymousState(String emailAddr) {
    doAssociate(emailAddr, false);
  }

  private void onSignInForce(String emailAddr) {
    doAssociate(emailAddr, true);
  }

  private void doAssociate(String emailAddr, boolean force) {
    Empire myEmpire = EmpireManager.i.getMyEmpire();
    log.info("Associating email address '%s' with empire #%d %s (force=%s)...",
        emailAddr, myEmpire.id, myEmpire.display_name, force ? "true" : "false");

    layout.updateState(
        R.string.signin_code_help,
        false /* signInEnabled */,
        R.string.signin,
        true /* isCancel */,
        false /* showEmailAddr */);

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
            () -> updateState(GameSettings.SignInState.AWAITING_VERIFICATION),
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
    if (msg == null) {
      switch (status) {
        case ACCOUNT_ALREADY_ASSOCIATED:
          layout.setForceSignIn(R.string.signin_error_account_already_exists);
          askingToForce = true;
          break;
        case EMAIL_ALREADY_ASSOCIATED:
          layout.setForceSignIn(R.string.signin_error_email_already_associated);
          askingToForce = true;
          break;
        case UNEXPECTED_ERROR:
        default:
          layout.setErrorMsg(R.string.signin_error_unknown);
          askingToForce = false;
          break;
      }
    } else {
      layout.setErrorMsg(msg);
    }
  }

  private void checkVerificationStatus() {
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
