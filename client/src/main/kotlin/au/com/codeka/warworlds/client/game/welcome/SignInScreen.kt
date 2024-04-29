package au.com.codeka.warworlds.client.game.welcome

import android.view.ViewGroup
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
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
 *
 * @param immediate if true, we'll assume you immediately clicked "sign in" to get started.
 */
class SignInScreen(private val immediate: Boolean) : Screen() {
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
    updateState()

    if (immediate) {
      performExplicitSignIn(GameSettings.getString(GameSettings.Key.COOKIE))
    }

    return ShowInfo.builder().view(layout).toolbarVisible(false).build()
  }

  /** Updates the current state of the UI based on whether you're logged in or not. */
  private fun updateState() {
    layout.setErrorMsg(null as String?)
    if (App.auth.isSignedIn) {
      layout.updateState(
          R.string.signin_switch_user_help,
          true /* signInEnabled */,
          R.string.switch_user,
          false /* isCancel */)
    } else {
      layout.updateState(
          R.string.signin_help,
          true /* signInEnabled */,
          R.string.signin,
          true /* isCancel */)
    }
  }

  private val layoutCallbacks: SignInLayout.Callbacks = object : SignInLayout.Callbacks {
    override fun onSignInClick() {
      if (App.auth.isSignedIn) {
        App.taskRunner.runOn(Threads.BACKGROUND) {
          App.auth.signOut()
        }.then(Threads.UI) {
          performExplicitSignIn("" /* cookie */)
        }
      } else {
        performExplicitSignIn(GameSettings.getString(GameSettings.Key.COOKIE))
      }
    }

    override fun onCreateEmpireClick() {
      context.pushScreen(CreateEmpireScreen())
    }

    override fun onCancelClick() {
      context.popScreen()
    }
  }

  private fun performExplicitSignIn(cookie: String) {
    val accountFuture = App.auth.explicitSignIn(context.activity)
    App.taskRunner.runOn(Threads.BACKGROUND) {
      val account = accountFuture.get()
      if (account == null) {
        // TODO: notify the user of whatever the error was.
        App.auth.signOut()

        return@runOn
      }

      doAssociate(account, cookie, false)
    }
  }

  /** Associate our account with the empire we're currently logged in as. */
  private fun doAssociate(account: GoogleSignInAccount, cookie: String, force: Boolean) {
    val myEmpire = if (EmpireManager.hasMyEmpire()) EmpireManager.getMyEmpire() else null
    log.info("Associating email address '%s' with empire #%d %s (force=%s)...",
        account.email, myEmpire?.id, myEmpire?.display_name, if (force) "true" else "false")
    App.taskRunner.runOn(Threads.UI) {
      layout.updateState(
          R.string.signin_pending_help,
          false /* signInEnabled */,
          R.string.signin,
          true /* isCancel */)
    }

    val request = HttpRequest.Builder()
        .url(url + "accounts/associate")
        .method(HttpRequest.Method.POST)
        .header("Content-Type", "application/x-protobuf")
        .body(AccountAssociateRequest(
            cookie = cookie, force = force, email_addr = account.email!!,
            id_token = account.idToken!!).encode())
        .build()
    val resp = request.getBody(AccountAssociateResponse::class.java)
    when {
      resp == null -> {
        // TODO: report the error
        log.error("Didn't get AccountAssociateResponse, as expected.", request.exception)
        App.taskRunner.runOn(Threads.UI) {
          onSignInError(
            AccountAssociateStatus.STATUS_UNKNOWN,
            account,
            "An unknown error occurred.")
        }
      }
      resp.status != AccountAssociateStatus.SUCCESS -> {
        App.taskRunner.runOn(Threads.UI) { onSignInError(resp.status!!, account, null) }
      }
      else -> {
        log.info("Associate successful")
        App.taskRunner.runOn(Threads.UI) {
          if (resp.cookie != null && resp.cookie != "") {
            log.info("Updating cookie: ${resp.cookie}")
            GameSettings.edit().setString(GameSettings.Key.COOKIE, resp.cookie!!).commit()
          }

          // After successfully associating, etc we'll go back to the welcome screen.
          context.home()
          context.pushScreen(WelcomeScreen())
        }
      }
    }
  }

  /** If you're verified, then hitting sign in again is for switching accounts.  */
  private fun onSignInVerifiedState() {
    // TODO
  }

  private fun onSignInError(
      status: AccountAssociateStatus,
      account: GoogleSignInAccount?,
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
        AccountAssociateStatus.NO_EMPIRE_FOR_ACCOUNT -> {
          layout.setCreateEmpireError(R.string.signin_error_no_empire, account?.email)
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

  companion object {
    private val log = Log("SignInScreen")
  }
}