package au.com.codeka.warworlds.client.game.welcome

import android.view.ViewGroup
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.TaskRunner
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.net.HttpRequest
import au.com.codeka.warworlds.client.net.ServerUrl.url
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.util.GameSettings
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.AccountAssociateRequest
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse.AccountAssociateStatus
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

/**
 * This screen is used to associate your cookie with an email address. We use Google Auth to
 * authenticate your email address with Google, then send it to the server for verification and
 * to associate it with the empire.
 */
class SignInScreen : Screen() {
  private lateinit var layout: SignInLayout
  private lateinit var context: ScreenContext

  /** If we're tried to associate and got an error that can be fixed by forcing, this'll be true. */
  private var askingToForce = false

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    this.context = context
    layout = SignInLayout(context.activity, layoutCallbacks)
  }

  override fun onShow(): ShowInfo? {
    updateState(null)
    return ShowInfo.builder().view(layout).toolbarVisible(false).build()
  }

  /** Updates the current state (or just refreshes the current state, if newState is null).  */
  private fun updateState(newState: GameSettings.SignInState?) {
    val signInState = if (newState != null) {
      GameSettings.edit()
          .setEnum(GameSettings.Key.SIGN_IN_STATE, newState)
          .commit()
      newState
    } else {
      GameSettings.getEnum(
          GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState::class.java)
    }
    layout.setErrorMsg(null as String?)
    when (signInState) {
      GameSettings.SignInState.ANONYMOUS -> layout.updateState(
          R.string.signin_help,
          true /* signInEnabled */,
          R.string.signin,
          true /* isCancel */)

      GameSettings.SignInState.VERIFIED -> layout.updateState(
          R.string.signin_switch_user_help,
          true /* signInEnabled */,
          R.string.switch_user,
          false /* isCancel */)
    }
  }

  private fun performExplicitSignIn() {
    val accountFuture = App.auth.explicitSignIn(context.activity)
    GameSettings.edit()
        .setEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.PENDING)
        .commit()
    App.taskRunner.runTask(Runnable {
      val account = accountFuture.get()
      if (account == null) {
        // Go back to anonymous, but record the error.
        GameSettings.edit()
            .setEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.ANONYMOUS)
            .commit()

        // TODO: notify the user of whatever the error was.
        return@Runnable
      }

      doAssociate(account, false)
    }, Threads.BACKGROUND)
  }

  private val layoutCallbacks: SignInLayout.Callbacks = object : SignInLayout.Callbacks {
    override fun onSignInClick() {
      if (App.auth.isSignedIn) {
        App.taskRunner.runTask(Runnable {
          App.auth.signOut()
        }, Threads.BACKGROUND).then( Runnable {
          performExplicitSignIn()
        }, Threads.UI)
      } else {
        performExplicitSignIn()
      }
    }

    override fun onCancelClick() {
      val signInState =
          GameSettings.getEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState::class.java)
      if (signInState !== GameSettings.SignInState.VERIFIED) {
        // if you're not yet verified, go back to the anonymous state.
        GameSettings.edit()
            .setEnum(GameSettings.Key.SIGN_IN_STATE, GameSettings.SignInState.ANONYMOUS)
            .commit()
      }

      //TODO getFragmentManager().popBackStack();
      context.popScreen()
    }
  }

  private fun doAssociate(account: GoogleSignInAccount, force: Boolean) {
    val myEmpire = if (EmpireManager.hasMyEmpire()) EmpireManager.getMyEmpire() else null
    log.info("Associating email address '%s' with empire #%d %s (force=%s)...",
        account.email, myEmpire?.id, myEmpire?.display_name, if (force) "true" else "false")
    App.taskRunner.runTask(Runnable {
      layout.updateState(
          R.string.signin_pending_help,
          false /* signInEnabled */,
          R.string.signin,
          true /* isCancel */)
    }, Threads.UI)

    val request = HttpRequest.Builder()
        .url(url + "accounts/associate")
        .method(HttpRequest.Method.POST)
        .header("Content-Type", "application/x-protobuf")
        .body(AccountAssociateRequest.Builder()
            .cookie(GameSettings.getString(GameSettings.Key.COOKIE))
            .force(force)
            .email_addr(account.email)
            .id_token(account.idToken)
            .build().encode())
        .build()
    val resp = request.getBody(AccountAssociateResponse::class.java)
    if (resp == null) {
      // TODO: report the error
      log.error("Didn't get AccountAssociateResponse, as expected.", request.exception)
      App.taskRunner.runTask(Runnable {
        onSignInError(
            AccountAssociateStatus.STATUS_UNKNOWN,
            "An unknown error occurred.")
      }, Threads.UI)
    } else if (resp.status != AccountAssociateStatus.SUCCESS) {
      App.taskRunner.runTask(Runnable { onSignInError(resp.status, null) }, Threads.UI)
    } else {
      log.info("Associate successful")
      App.taskRunner.runTask(
          Runnable {
            if (resp.cookie != null && resp.cookie != "") {
              log.info("Updating cookie: ${resp.cookie}")
              GameSettings.edit().setString(GameSettings.Key.COOKIE, resp.cookie).commit()
            }
            updateState(GameSettings.SignInState.VERIFIED)
          },
          Threads.UI)
    }
  }

  /** If you're verified, then hitting sign in again is for switching accounts.  */
  private fun onSignInVerifiedState() {
    // TODO
  }

  private fun onSignInError(
      status: AccountAssociateStatus,
      msg: String?) {
    if (msg == null) {
      askingToForce = when (status) {
        AccountAssociateStatus.ACCOUNT_ALREADY_ASSOCIATED -> {
          layout.setForceSignIn(R.string.signin_error_account_already_exists)
          true
        }
        AccountAssociateStatus.EMAIL_ALREADY_ASSOCIATED -> {
          layout.setForceSignIn(R.string.signin_error_email_already_associated)
          true
        }
        AccountAssociateStatus.UNEXPECTED_ERROR -> {
          layout.setErrorMsg(R.string.signin_error_unknown)
          false
        }
        else -> {
          layout.setErrorMsg(R.string.signin_error_unknown)
          false
        }
      }
    } else {
      layout.setErrorMsg(msg)
    }
  }

  private fun checkVerificationStatus() {
    val myEmpire = EmpireManager.getMyEmpire()
    App.taskRunner.runTask(Runnable {
      val request = HttpRequest.Builder()
          .url(url + "accounts/associate?id=" + myEmpire.id)
          .method(HttpRequest.Method.GET)
          .build()
      val resp = request.getBody(AccountAssociateResponse::class.java)
      if (resp == null) {
        // TODO: report the error
        log.error("Didn't get AccountAssociateResponse, as expected.", request.exception)
        App.taskRunner.runTask(Runnable {
          onSignInError(
              AccountAssociateStatus.STATUS_UNKNOWN,
              "An unknown error occurred.")
        },
            Threads.UI)
      } else if (resp.status != AccountAssociateStatus.SUCCESS) {
        // Just wait.
      } else {
        log.info("Associate successful, verification complete.")
        App.taskRunner.runTask(
            Runnable { updateState(GameSettings.SignInState.VERIFIED) },
            Threads.UI)
      }
    }, Threads.BACKGROUND)
  }

  companion object {
    private val log = Log("SignInScreen")
  }
}