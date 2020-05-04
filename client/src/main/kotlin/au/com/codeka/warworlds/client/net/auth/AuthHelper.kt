package au.com.codeka.warworlds.client.net.auth

import android.app.Activity
import android.content.Context
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.common.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.Futures
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

const val SERVER_CLIENT_ID =
    "809181406384-obrefe4qb0hmaektbesqsc25rop0u72f.apps.googleusercontent.com"

const val SIGN_IN_COMPLETE_RESULT_CODE = 9746

class AuthHelper(private val appContext: Context) {
  private lateinit var client: GoogleSignInClient

  private val awaitingSignInCompleteLock = Object()
  private var awaitingSignInComplete = false

  /**
   * The current [GoogleSignInAccount], or null if not signed in. This can also be null before
   * [silentSignIn] has completed. If you need to know whether [silentSignIn] has completed (and
   * the account could still be null), use [futureAccount].
   */
  var account: GoogleSignInAccount? = null
    private set

  companion object {
    val log = Log("AuthHelper")
  }

  /** Returns a future that will only return after [silentSignIn] has completed. */
  fun futureAccount(): Future<GoogleSignInAccount?> {
    if (awaitingSignInComplete) {
      return Futures.immediateFuture(account)
    }

    return App.taskRunner.backgroundExecutor.submit(Callable {
      while (!awaitingSignInComplete) {
        synchronized(awaitingSignInCompleteLock) {
          awaitingSignInCompleteLock.wait(100)
        }
      }
      account
    })
  }

  val isSignedIn: Boolean
    get() = account != null

  /**
   * Attempt to do a 'silent' sign-in. This should be called as early in app start-up time as
   * possible.
   */
  fun silentSignIn(activity: Activity) {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(SERVER_CLIENT_ID)
        .requestEmail()
        .build()
    client = GoogleSignIn.getClient(appContext, gso)
    client.silentSignIn().addOnCompleteListener(activity) { task -> handleSignInResult(task) }
  }

  /**
   * Perform an explicit sign-in. This is after the user has clicked a "sign in" button and they
   * expect to see some UI.
   */
  fun explicitSignIn(activity: Activity): Future<GoogleSignInAccount?> {
    awaitingSignInComplete = false
    activity.startActivityForResult(client.signInIntent, SIGN_IN_COMPLETE_RESULT_CODE)
    return futureAccount()
  }

  /**
   * Handle a sign-in result. This is called from the activity's onActivityResult when you get
   * a result code of [SIGN_IN_COMPLETE_RESULT_CODE].
   */
  fun handleSignInResult(task: Task<GoogleSignInAccount>) {
    try {
      val acct = task.getResult(ApiException::class.java)
      account = acct
      log.info("Authenticated as: ${acct.email} : ${acct.displayName}")
      App.eventBus.publish(AuthAccountUpdate(acct))
    } catch (e: ApiException) {
      log.warning("Authentication failed code: ${e.statusCode}")
      account = null
      App.eventBus.publish(AuthAccountUpdate(null))
    }

    synchronized(awaitingSignInCompleteLock) {
      if (!awaitingSignInComplete) {
        awaitingSignInComplete = true
        awaitingSignInCompleteLock.notifyAll()
      }
    }
  }
}
