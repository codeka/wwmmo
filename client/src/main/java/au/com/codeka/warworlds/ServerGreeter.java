package au.com.codeka.warworlds;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.safetynet.SafetyNetClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.annotation.Nullable;

import au.com.codeka.ErrorReporter;
import au.com.codeka.common.Log;
import au.com.codeka.common.safetynet.NonceBuilder;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.concurrency.Threads;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;

/**
 * This class is used to make sure we're said "Hello" to the server and that we've got our
 * empire and stuff all set up.
 */
public class ServerGreeter {
  private static final Log log = new Log("ServerGreeter");
  private static final ArrayList<HelloCompleteHandler> helloCompleteHandlers;
  private static final ArrayList<HelloWatcher> helloWatchers;
  private static boolean helloStarted;
  private static boolean helloComplete;
  private static boolean helloSuccessful;
  private static ServerGreeting serverGreeting;
  private static Messages.HelloResponse helloResponsePb;

  private static final String SAFETYNET_CLIENT_API_KEY = "AIzaSyAulj6q4uq0fd7WnJpzSY769U0aMCthogg";

  /**
   * When we change realms, we'll want to make sure we say 'hello' again.
   */
  private static final RealmManager.RealmChangedHandler realmChangedHandler =
      newRealm -> clearHello();

  static {
    helloCompleteHandlers = new ArrayList<>();
    helloWatchers = new ArrayList<>();
    clearHello();
    RealmManager.i.addRealmChangedHandler(realmChangedHandler);
  }

  public static void addHelloWatcher(HelloWatcher watcher) {
    synchronized (helloWatchers) {
      helloWatchers.add(watcher);
    }
  }

  public static void removeHelloWatcher(HelloWatcher watcher) {
    synchronized (helloWatchers) {
      helloWatchers.remove(watcher);
    }
  }

  /**
   * Resets the fact that we've said hello, and causes a new 'hello' to be issued.
   */
  public static void clearHello() {
    helloStarted = false;
    helloComplete = false;
    serverGreeting = new ServerGreeting();

    // tell the empire manager that the "MyEmpire" it has cached will no longer be valid either.
    EmpireManager.i.clearEmpire();

    BackgroundDetector.i.resetBackStack();
  }

  /** Gets the hello response we got from the server (or null if we haven't finished the hello). */
  @Nullable
  public static Messages.HelloResponse getHelloResponse() {
    return helloResponsePb;
  }

  /**
   * Wait for hello.
   *
   * @param activity The {@link MainActivity} that we'll use as context. If you don't want to actually
   *                 start the hello process, you can pass null for this.
   * @param handler A callback that will be called when successfully connected to the server.
   */
  public static void waitForHello(@Nullable MainActivity activity, HelloCompleteHandler handler) {
    if (helloComplete) {
      log.debug("Already said 'hello', not saying it again...");
      handler.onHelloComplete(helloSuccessful, serverGreeting);
      return;
    }
    log.debug("Hello hasn't completed, waiting for hello.");

    synchronized (helloCompleteHandlers) {
      helloCompleteHandlers.add(handler);

      if (!helloStarted && activity != null) {
        helloStarted = true;
        sayHello(activity, 0);
      }
    }
  }

  private static void sayHello(@NonNull final MainActivity activity, final int retries) {
    Util.setup(activity);
    log.debug("Saying 'hello'...");

    Util.loadProperties();
    if (Util.isDebug()) {
      enableStrictMode();
    }

    PreferenceManager.setDefaultValues(activity, R.xml.global_options, false);

    ActivityManager activityManager =
        (ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE);
    if (activityManager != null) {
      int memoryClass = activityManager.getMemoryClass();
      if (memoryClass < 40) {
        // on low memory devices, we want to make sure the background detail is always BLACK
        // this is a bit of a hack, but should stop the worst of the memory issues (I hope!)
        new GlobalOptions().setStarfieldDetail(GlobalOptions.StarfieldDetail.BLACK);
      }
    }

    SharedPreferences prefs = Util.getSharedPreferences();
    String accountName = prefs.getString("AccountName", null);
    if (accountName == null) {
      // If we don't have an account name, just generate a random one that we'll be able to use
      // (though on this device only, and assuming you never clear your data!)
      accountName = generateAnonUserName();
      prefs.edit().putString("AccountName", accountName).apply();
    }

    // Whenever an accessibility service is enabled or disabled, we'll want to clear the hello so
    // that the server is able to verify that it's a supported/allowed service.
    AccessibilityServiceReporter.watchForChanges(activity, () -> {
      log.info("Got a change in accessibility services, clearing hello.");
      clearHello();
    });

    // We'll start this now so it can go on in the background while we do some other stuff.
    // TODO: should we do this every login, or maybe only max once a day or something (on this
    // device anyway)?
    byte[] nonce = new NonceBuilder()
        .empireID(/* TODO? */ 0)
        .build();
    SafetyNetClient client = SafetyNet.getClient(activity);
    Task<SafetyNetApi.AttestationResponse> task = client.attest(nonce, SAFETYNET_CLIENT_API_KEY);

    App.i.getTaskRunner().runTask(() -> {
      HelloResult res = new HelloResult();

      App.i.getTaskRunner().runTask(() -> {
        synchronized (helloWatchers) {
          for (HelloWatcher watcher : helloWatchers) {
            watcher.onAuthenticating();
          }
        }
      }, Threads.UI);

      // Check that you have a version of Google Play Services installed that we can use (in
      // particular, for the SafetyNet API). Version 13.0 added support for restricted API keys,
      // which we use, so make sure it's at least 13.0.
      if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity, 13000000)
          != ConnectionResult.SUCCESS) {
        res.errorOccurred = true;
        res.giveUpReason = GiveUpReason.GOOGLE_PLAY_SERVICES;
        res.msg = "The version of Google Play Services you have needs to be updated.";
        return res;
      }

      Realm realm = RealmContext.i.getCurrentRealm();
      if (!realm.getAuthenticator().isAuthenticated()) {
        try {
          log.info("Not authenticated, re-authenticating.");
          realm.getAuthenticator().authenticate(activity, realm);
        } catch (ApiException e) {
          res.errorOccurred = true;
          // if it wasn't a network error, it probably means we need to re-auth.
          res.needsReAuthenticate = !e.networkError();
          if (e.getCause() instanceof UserRecoverableAuthException) {
            res.intent = ((UserRecoverableAuthException) e.getCause()).getIntent();
          }
          if (e.getServerErrorCode() > 0 && e.getServerErrorMessage() != null) {
            res.toastMessage = e.getServerErrorMessage();
          }
          return res;
        }
      }

      // Schedule registration with FCM, which will update our device when we get the
      // registration ID
      App.i.getNotificationManager().setup();
      String deviceRegistrationKey = DeviceRegistrar.getDeviceRegistrationKey();
      if (deviceRegistrationKey == null || deviceRegistrationKey.length() == 0) {
        try {
          deviceRegistrationKey = DeviceRegistrar.register();
        } catch (ApiException e) {
          res.errorOccurred = true;
          // only re-authenticate for non-network related errors
          res.needsReAuthenticate = !e.networkError();
          if (e.getServerErrorCode() > 0 && e.getServerErrorMessage() != null) {
            res.toastMessage = e.getServerErrorMessage();
          }
          return res;
        }
      }

      App.i.getTaskRunner().runTask(() -> {
        synchronized (helloWatchers) {
          for (HelloWatcher watcher : helloWatchers) {
            watcher.onConnecting();
          }
        }
      }, Threads.UI);

      String safetyNetAttestationJwsResult;
      try {
        SafetyNetApi.AttestationResponse safetyNetAttestation = Tasks.await(task);
        log.info("SafetyNet attestation: %s", safetyNetAttestation.getJwsResult());
        safetyNetAttestationJwsResult = safetyNetAttestation.getJwsResult();
      } catch (Exception e) {
        // TODO: retry?
        log.error("SafetyNet attestation error.", e);
        safetyNetAttestationJwsResult = "ERROR:" + e.toString();
      }

      // say hello to the server
      int memoryClass = ((ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE))
          .getMemoryClass();
      Messages.HelloRequest req =
          Messages.HelloRequest.newBuilder()
              .setDeviceBuild(Build.DISPLAY)
              .setDeviceManufacturer(Build.MANUFACTURER)
              .setDeviceModel(Build.MODEL)
              .setDeviceVersion(Build.VERSION.RELEASE).setMemoryClass(memoryClass)
              .setAllowInlineNotfications(false)
              .setNoStarList(true)
              .setAccessibilitySettingsInfo(AccessibilityServiceReporter.get(activity))
              .setSafetynetJwsResult(safetyNetAttestationJwsResult)
              .setSafetynetNonce(ByteString.copyFrom(nonce))
              .build();

      String url = "hello/" + deviceRegistrationKey;
      ApiRequest request = new ApiRequest.Builder(url, "PUT").body(req).build();
      RequestManager.i.sendRequestSync(request);
      if (request.error() == null) {
        Messages.HelloResponse resp = request.body(Messages.HelloResponse.class);
        helloResponsePb = resp;
        if (resp == null) {
          res.errorOccurred = true;
          res.msg = "Unknown error";
          return res;
        }

        FeatureFlags.setup(helloResponsePb.getFeatureFlags());

        if (resp.hasEmpire()) {
          res.needsEmpireSetup = false;
          MyEmpire myEmpire = new MyEmpire();
          myEmpire.fromProtocolBuffer(resp.getEmpire());
          EmpireManager.i.setup(myEmpire);
        } else {
          res.needsEmpireSetup = true;
        }

        if (resp.hasWasEmpireReset() && resp.getWasEmpireReset()) {
          res.wasEmpireReset = true;
          if (resp.hasEmpireResetReason() && resp.getEmpireResetReason().length() > 0) {
            res.resetReason = resp.getEmpireResetReason();
          }
        }

        if (resp.hasRequireGcmRegister() && resp.getRequireGcmRegister()) {
          log.info("Re-registering for GCM...");
          //GCMIntentService.register(activity);
          // we can keep going, though...
        }

        BuildManager.i.setup(resp.getBuildingStatistics());

        res.msg = resp.getMotd().getMessage();
        res.errorOccurred = false;
      } else {
        log.error("Error occurred in 'hello': %s", request.error().getErrorMessage());
        res.giveUpReason = GiveUpReason.NONE;

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        Throwable exception = request.exception();
        if (exception != null
              && (exception.getCause() instanceof UserRecoverableAuthException)) {
          res.msg = "Authentication failed.";
          res.errorOccurred = true;
          res.needsReAuthenticate = true;
          res.intent = ((UserRecoverableAuthException) exception.getCause()).getIntent();
        } else if (exception != null) {
          // if there's no status message, it likely means we were unable to connect
          // (i.e. a network error) just keep retrying until it works.
          res.msg = "An error occurred talking to the server, check data connection.";
          res.errorOccurred = true;
          res.needsReAuthenticate = false;
        } else if (request.error().getErrorCode()
            == Messages.GenericError.ErrorCode.UpgradeRequired.getNumber()) {
          res.msg = request.error().getErrorMessage().replace("\\n", "\n");
          res.giveUpReason = GiveUpReason.UPGRADE_REQUIRED;
          res.errorOccurred = true;
          res.needsReAuthenticate = false;
        } else if (request.error().getErrorCode()
            == Messages.GenericError.ErrorCode.AuthenticationError.getNumber()) {
          // if it's an authentication problem, we'll want to re-authenticate
          res.msg = "Authentication failed.";
          res.errorOccurred = true;
          res.needsReAuthenticate = true;
        } else if (request.error().getErrorCode()
            == Messages.GenericError.ErrorCode.ClientDeviceRejected.getNumber()) {
          // the client was rejected for some reason (e.g. auto-clicker installed, failed
          // SafetyNet validation, etc).
          res.msg = request.error().getErrorMessage();
          res.giveUpReason = GiveUpReason.CLIENT_REJECTED;
          res.errorOccurred = true;
          res.needsReAuthenticate = false;
        } else {
          // any other HTTP error, let's display that
          res.msg = "An unexpected error occurred:" + request.error().getErrorCode();
          res.errorOccurred = true;
          res.needsReAuthenticate = false;
        }
      }

      return res;
    }, Threads.BACKGROUND).then((res) -> {
        if (res.needsEmpireSetup) {
// TODO          serverGreeting.mIntent = new Intent(activity, EmpireSetupActivity.class);
          helloComplete = true;
          helloSuccessful = true;
        } else if (!res.errorOccurred) {
          Util.setup(activity);
          ChatManager.i.setup();
          App.i.getNotificationManager().startLongPoll();

          // make sure we're correctly registered as online.
          BackgroundDetector.i.onBackgroundStatusChange();

          helloComplete = true;
          helloSuccessful = true;
        } else /* errorOccurred */ {
          helloSuccessful = false;

          if (res.toastMessage != null && res.toastMessage.length() > 0) {
            Toast toast = Toast.makeText(App.i, res.toastMessage, Toast.LENGTH_LONG);
            toast.show();
          }

          if (res.needsReAuthenticate) {
            // if we need to re-authenticate, first forget the current credentials
            // the switch to the AccountsActivity.
            String currAccountName = prefs.getString("AccountName", null);
            if (currAccountName != null && currAccountName.endsWith("@anon.war-worlds.com")) {
              log.error("Need to re-authenticate, but AccountName is anonymous, cannot re-auth.");
            } else {
              SharedPreferences.Editor editor = prefs.edit();
              editor.remove("AccountName");
              editor.apply();

              if (res.intent != null) {
                serverGreeting.intent = res.intent;
              } else {
                serverGreeting.fragmentID = R.id.accountsFragment;
              }
            }
            helloComplete = true;
          } else if (res.giveUpReason != GiveUpReason.NONE) {
            synchronized (helloWatchers) {
              for (HelloWatcher watcher : helloWatchers) {
                watcher.onFailed(res.msg, res.giveUpReason);
              }
            }
            helloComplete = true;
          } else {
            // otherwise, just try again
            synchronized (helloWatchers) {
              for (HelloWatcher watcher : helloWatchers) {
                watcher.onFailed(res.msg, GiveUpReason.NONE);
              }
            }

            App.i.getTaskRunner().runTask(() -> {
              synchronized (helloWatchers) {
                for (HelloWatcher watcher : helloWatchers) {
                  watcher.onRetry(retries + 1);
                }
              }

              sayHello(activity, retries + 1);
            }, Threads.UI, 3000);
            helloComplete = false;
          }
        }

        if (res.wasEmpireReset) {
          if (res.resetReason != null && res.resetReason.equals("blitz")) {
// TODO            serverGreeting.mIntent = new Intent(activity, BlitzResetActivity.class);
          } else {
// TODO            serverGreeting.mIntent = new Intent(activity, EmpireResetActivity.class);
            if (res.resetReason != null) {
// TODO              serverGreeting.mIntent.putExtra(
//                  "au.com.codeka.warworlds.ResetReason", res.resetReason);
            }
          }
        }

        if (helloComplete) {
          synchronized (helloCompleteHandlers) {
            for (HelloCompleteHandler handler : helloCompleteHandlers) {
              handler.onHelloComplete(!res.errorOccurred, serverGreeting);
            }
            helloCompleteHandlers.clear();
          }

          if (serverGreeting.fragmentID > 0) {
            activity.getNavController().navigate(serverGreeting.fragmentID);
          }
          if (serverGreeting.intent != null) {
            activity.startActivity(serverGreeting.intent);
          }

          ErrorReporter.register(activity);
        }
      }, Threads.UI);
  }

  private static final class HelloResult {
    public String msg;
    public boolean needsEmpireSetup;
    public boolean errorOccurred;
    public boolean needsReAuthenticate;
    public boolean wasEmpireReset;
    public GiveUpReason giveUpReason;
    public String resetReason;
    public String toastMessage;
    public Intent intent;
  }

  @SuppressLint({"NewApi"}) // StrictMode doesn't work on < 3.0 and some of the tests are even newer
  private static void enableStrictMode() {
    try {
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork()
              .penaltyLog().build());
      StrictMode.setVmPolicy(
          new StrictMode.VmPolicy.Builder().detectActivityLeaks().detectLeakedClosableObjects()
              .detectLeakedRegistrationObjects().detectLeakedSqlLiteObjects().penaltyLog()
              // TODO: ads in google play services leak resources :\
              //.penaltyDeath() // these are bad enough to warrent death...
              .build());

      // Replace System.err with one that'll monitor for StrictMode killing us and
      // perform a hprof heap dump just before it does.
      System.setErr(new PrintStreamThatDumpsHprofWhenStrictModeKillsUs(System.err));
    } catch (Exception e) {
      // ignore errors
    }
  }

  /**
   * This is quite a hack, but we want a heap dump when strict mode is about to kill us,
   * so we monitor System.err for the message from StrictMode that it's going to do that
   * and then do a manual heap dump.
   */
  private static class PrintStreamThatDumpsHprofWhenStrictModeKillsUs extends PrintStream {
    public PrintStreamThatDumpsHprofWhenStrictModeKillsUs(OutputStream outs) {
      super(outs);
    }

    @Override
    public synchronized void println(String str) {
      super.println(str);
      if (str.equals("StrictMode VmPolicy violation with POLICY_DEATH; shutting down.")) {
        // StrictMode is about to terminate us... do a heap dump!
        try {
          File dir = Environment.getExternalStorageDirectory();
          File file = new File(dir, "wwmmo-strictmode-violation.hprof");
          super.println("Dumping HPROF to: " + file);
          Debug.dumpHprofData(file.getAbsolutePath());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /** Generates a user-name that an anonymous user can use. */
  private static String generateAnonUserName() {
    char[] validChars= "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    SecureRandom rand = new SecureRandom();
    StringBuilder userName = new StringBuilder();
    for (int i = 0; i < 64; i++) {
      userName.append(validChars[rand.nextInt(validChars.length)]);
    }
    return userName + "@anon.war-worlds.com";
  }

  public static class ServerGreeting {
    private Intent intent;
    private int fragmentID;

    public int getFragmentID() {
      return fragmentID;
    }

    public Intent getIntent() {
      return intent;
    }
  }

  public interface HelloCompleteHandler {
    void onHelloComplete(boolean success, ServerGreeting greeting);
  }

  public interface HelloWatcher {
    void onAuthenticating();

    void onConnecting();

    /**
     * Called when a failure occurs. If the GiveUpReason is NONE then we'll retry in a bit (and
     * call {@link #onRetry(int)} when we do). Otherwise, we're giving up.
     */
    void onFailed(String message, GiveUpReason reason);

    void onRetry(int retries);
  }

  public enum GiveUpReason {
    /** We're not giving up. */
    NONE,

    /** You need to upgrade your version of the game client. Link to the Play Store. */
    UPGRADE_REQUIRED,

    /** The version of Google Play Services that's installed is not one we are able to use. */
    GOOGLE_PLAY_SERVICES,

    /**
     * This device failed SafetyNet attestation, or a clicker was detected, or some other reason
     * for rejecting the client.
     */
    CLIENT_REJECTED,
  }
}
