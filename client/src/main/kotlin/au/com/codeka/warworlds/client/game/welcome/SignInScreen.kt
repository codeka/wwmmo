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
import au.com.codeka.warworlds.client.util.GameSettings.SignInState
import au.com.codeka.warworlds.client.util.GameSettings.edit
import au.com.codeka.warworlds.client.util.GameSettings.getEnum
import au.com.codeka.warworlds.client.util.GameSettings.getString
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.AccountAssociateRequest
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse.AccountAssociateStatus

/**
 * This screen is used to associate your cookie with an email address. You'll get a code in your
 * email account that you then have to type into the box.
 */
class SignInScreen : Screen() {
  private var layout: SignInLayout? = null
  private var context: ScreenContext? = null

  /** If we're tried to associate and got an error that can be fixed by forcing, this'll be true.  */
  private var askingToForce = false
  override fun onCreate(context: ScreenContext?, container: ViewGroup?) {
    super.onCreate(context, container)
    this.context = context
    layout = SignInLayout(context!!.activity, layoutCallbacks)
  }

  override fun onShow(): ShowInfo? {
    updateState(null)
    val signInState = getEnum(GameSettings.Key.SIGN_IN_STATE, SignInState::class.java)
    if (signInState === SignInState.AWAITING_VERIFICATION) {
      checkVerificationStatus()
    }
    return ShowInfo.builder().view(layout).toolbarVisible(false).build()
  }

  /** Updates the current state (or just refreshes the current state, if newState is null).  */
  private fun updateState(newState: SignInState?) {
    val signInState: SignInState
    signInState = if (newState != null) {
      edit()
          .setEnum(GameSettings.Key.SIGN_IN_STATE, newState)
          .commit()
      newState
    } else {
      getEnum(
          GameSettings.Key.SIGN_IN_STATE, SignInState::class.java)
    }
    layout!!.setErrorMsg(null as String?)
    when (signInState) {
      SignInState.ANONYMOUS -> layout!!.updateState(
          R.string.signin_help,
          true /* signInEnabled */,
          R.string.signin,
          true /* isCancel */,
          true /* showEmailAddr */)
      SignInState.AWAITING_VERIFICATION -> layout!!.updateState(
          R.string.signin_code_help,
          false /* signInEnabled */,
          R.string.signin,
          true /* isCancel */,
          false /* showEmailAddr */)
      SignInState.VERIFIED -> layout!!.updateState(
          R.string.signin_complete_help,
          true /* signInEnabled */,
          R.string.switch_user,
          false /* isCancel */,
          false /* showEmailAddr */)
      else -> throw IllegalStateException("Unexpected sign-in state: $signInState")
    }
  }

  private val layoutCallbacks: SignInLayout.Callbacks = object : SignInLayout.Callbacks {
    override fun onSignInClick(emailAddr: String) {
      if (askingToForce) {
        onSignInForce(emailAddr)
        return
      }
      val signInState = getEnum(GameSettings.Key.SIGN_IN_STATE, SignInState::class.java)
      when (signInState) {
        SignInState.ANONYMOUS -> onSignInAnonymousState(emailAddr)
        SignInState.AWAITING_VERIFICATION -> {
        }
        SignInState.VERIFIED -> onSignInVerifiedState()
        else -> throw IllegalStateException("Unexpected sign-in state: $signInState")
      }
    }

    override fun onCancelClick() {
      val signInState = getEnum(GameSettings.Key.SIGN_IN_STATE, SignInState::class.java)
      if (signInState !== SignInState.VERIFIED) {
        // if you're not yet verified, go back to the anonymous state.
        edit()
            .setEnum(GameSettings.Key.SIGN_IN_STATE, SignInState.ANONYMOUS)
            .commit()
      }
      layout!!.hideKeyboard()

      //TODO getFragmentManager().popBackStack();
      context!!.popScreen()
    }
  }

  private fun onSignInAnonymousState(emailAddr: String) {
    doAssociate(emailAddr, false)
  }

  private fun onSignInForce(emailAddr: String) {
    doAssociate(emailAddr, true)
  }

  private fun doAssociate(emailAddr: String, force: Boolean) {
    val myEmpire = EmpireManager.getMyEmpire()
    log.info("Associating email address '%s' with empire #%d %s (force=%s)...",
        emailAddr, myEmpire.id, myEmpire.display_name, if (force) "true" else "false")
    layout!!.updateState(
        R.string.signin_code_help,
        false /* signInEnabled */,
        R.string.signin,
        true /* isCancel */,
        false /* showEmailAddr */)
    App.taskRunner.runTask(Runnable {
      val request = HttpRequest.Builder()
          .url(url + "accounts/associate")
          .method(HttpRequest.Method.POST)
          .header("Content-Type", "application/x-protobuf")
          .body(AccountAssociateRequest.Builder()
              .cookie(getString(GameSettings.Key.COOKIE))
              .force(force)
              .email_addr(emailAddr)
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
        },
            Threads.UI)
      } else if (resp.status != AccountAssociateStatus.SUCCESS) {
        App.taskRunner.runTask(Runnable { onSignInError(resp.status, null) }, Threads.UI)
      } else {
        log.info("Associate successful, awaiting verification code")
        App.taskRunner.runTask(
            Runnable { updateState(SignInState.AWAITING_VERIFICATION) },
            Threads.UI)
      }
    }, Threads.BACKGROUND)
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
          layout!!.setForceSignIn(R.string.signin_error_account_already_exists)
          true
        }
        AccountAssociateStatus.EMAIL_ALREADY_ASSOCIATED -> {
          layout!!.setForceSignIn(R.string.signin_error_email_already_associated)
          true
        }
        AccountAssociateStatus.UNEXPECTED_ERROR -> {
          layout!!.setErrorMsg(R.string.signin_error_unknown)
          false
        }
        else -> {
          layout!!.setErrorMsg(R.string.signin_error_unknown)
          false
        }
      }
    } else {
      layout!!.setErrorMsg(msg)
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
            Runnable { updateState(SignInState.VERIFIED) },
            Threads.UI)
      }
    }, Threads.BACKGROUND)
  }

  companion object {
    private val log = Log("SignInScreen")
  }
}