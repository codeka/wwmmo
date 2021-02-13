package au.com.codeka.warworlds.api.greeter;

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
import au.com.codeka.warworlds.AccessibilityServiceReporter;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.DeviceRegistrar;
import au.com.codeka.warworlds.EmpireResetFragmentArgs;
import au.com.codeka.warworlds.FeatureFlags;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.MainActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.concurrency.TaskRunner;
import au.com.codeka.warworlds.concurrency.Threads;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;

/**
 * This class is used to make sure we're said "Hello" to the server and that we've got our
 * empire and stuff all set up. We are started by the MainActivity and monitored from
 * WelcomeFragment.
 */
public class ServerGreeter {
  private static final Log log = new Log("ServerGreeter");

  private static final int RETRY_DELAY_MS = 1000;

  private MainActivity activity;
  private Messages.HelloResponse helloResponsePb;

  private byte[] safetyNetNonce;
  private Task<SafetyNetApi.AttestationResponse> safetyNetTask;

  @NonNull private GreetingWatcher greetingWatcher = new NoopGreetingWatcher();

  private static final String SAFETYNET_CLIENT_API_KEY = "AIzaSyAulj6q4uq0fd7WnJpzSY769U0aMCthogg";

  public void setGreetingWatcher(@Nullable GreetingWatcher watcher) {
    if (watcher == null) {
      greetingWatcher = new NoopGreetingWatcher();
    } else {
      greetingWatcher = watcher;
    }
  }

  public void clearHello() {
    helloResponsePb = null;

    App.i.getTaskRunner().runTask(this::attemptHello, Threads.BACKGROUND);
  }

  public void sayHello(@NonNull final MainActivity activity) {
    this.activity = activity;
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

    // Whenever an accessibility service is enabled or disabled, we'll want to clear the hello so
    // that the server is able to verify that it's a supported/allowed service.
    AccessibilityServiceReporter.watchForChanges(activity, () -> {
      log.info("Got a change in accessibility services, clearing hello.");
      clearHello();
    });

    // We'll start this now so it can go on in the background while we do some other stuff.
    // TODO: should we do this every login, or maybe only max once a day or something (on this
    // device anyway)?
    safetyNetNonce = new NonceBuilder()
        .empireID(/* TODO? */ 0)
        .build();
    SafetyNetClient client = SafetyNet.getClient(activity);
    safetyNetTask = client.attest(safetyNetNonce, SAFETYNET_CLIENT_API_KEY);

    App.i.getTaskRunner().runTask(this::attemptHello, Threads.BACKGROUND);
  }

  private void attemptHello() {
    try {
      doHello(0);
    } catch (Exception e) {
      scheduleRetry("Unexpected error", 0);
    }
  }

  private void scheduleRetry(String errorMsg, int retries) {
    App.i.getTaskRunner().runTask(
        () -> greetingWatcher.onFailed(errorMsg, GiveUpReason.NONE), Threads.UI);
    App.i.getTaskRunner().runTask(() -> {
      App.i.getTaskRunner().runTask(() -> greetingWatcher.onRetry(retries + 1), Threads.UI);
      try {
        doHello(retries + 1);
      } catch (Exception e) {
        scheduleRetry("Unexpected error", retries + 1);
      }
    }, Threads.BACKGROUND, RETRY_DELAY_MS);
  }

  private void silentRetry() {
    App.i.getTaskRunner().runTask(() -> {
      try {
        doHello(0);
      } catch (Exception e) {
        scheduleRetry("Unexpected error", 0);
      }
    }, Threads.BACKGROUND, RETRY_DELAY_MS);
  }

  private void doHello(int retries) {
    Threads.checkOnThread(Threads.BACKGROUND);

    TaskRunner tr = App.i.getTaskRunner();
    tr.runTask(() -> greetingWatcher.onAuthenticating(), Threads.UI);

    // Check that you have a version of Google Play Services installed that we can use (in
    // particular, for the SafetyNet API). Version 13.0 added support for restricted API keys,
    // which we use, so make sure it's at least 13.0.
    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity, 13000000)
        != ConnectionResult.SUCCESS) {
      String msg = "The version of Google Play Services you have needs to be updated.";
      tr.runTask(
          () -> greetingWatcher.onFailed(msg, GiveUpReason.GOOGLE_PLAY_SERVICES), Threads.UI);
      return;
    }

    Realm realm = RealmContext.i.getCurrentRealm();
    if (realm == null) {
      // We haven't picked a realm yet.
      silentRetry();
      return;
    }
    String accountName = getAccountName();
    if (!realm.getAuthenticator().isAuthenticated()) {
      try {
        log.info("Not authenticated, re-authenticating.");
        realm.getAuthenticator().authenticate(activity, realm);
      } catch (ApiException e) {
        log.error("Auth error: ", e);

        // if it was a network error, just try again in a little bit.
        if (e.networkError()) {
          scheduleRetry("Network error", retries);
          return;
        }

        // OK, authentication error.
        if (accountName.endsWith("@anon.war-worlds.com")) {
          log.error("Need to re-authenticate, but AccountName is anonymous, cannot re-auth.");
          tr.runTask(
              () -> greetingWatcher.onFailed("Unexpected error.", GiveUpReason.UNEXPECTED_ERROR),
              Threads.UI);
        } else if (e.getCause() instanceof UserRecoverableAuthException) {
          Intent intent = ((UserRecoverableAuthException) e.getCause()).getIntent();
          tr.runTask(() -> activity.startActivity(intent), Threads.UI);
          clearHello();
        } else if (e.getServerErrorCode() > 0 && e.getServerErrorMessage() != null) {
          scheduleRetry(e.getServerErrorMessage(), retries);
        } else {
          // Some other error?
          scheduleRetry("Unexpected error", retries);
        }
        return;
      }
    }

    // Schedule registration with FCM, which will update our device when we get the registration ID.
    App.i.getNotificationManager().setup();
    String deviceRegistrationKey = DeviceRegistrar.getDeviceRegistrationKey();
    if (deviceRegistrationKey == null || deviceRegistrationKey.length() == 0) {
      try {
        deviceRegistrationKey = DeviceRegistrar.register();
      } catch (ApiException e) {
        scheduleRetry("Device registration error", retries);
        return;
      }
    }

    App.i.getTaskRunner().runTask(() -> greetingWatcher.onConnecting(), Threads.UI);

    // TODO: better SafetyNet support (don't block logging in while waiting for SafetyNet)
    String safetyNetAttestationJwsResult = "";
/*
    try {
      long startTime = System.currentTimeMillis();
      SafetyNetApi.AttestationResponse safetyNetAttestation = Tasks.await(safetyNetTask);
      log.info("SafetyNet attestation complete after %dms", System.currentTimeMillis() - startTime);
      safetyNetAttestationJwsResult = safetyNetAttestation.getJwsResult();
    } catch (Exception e) {
      // TODO: retry?
      log.error("SafetyNet attestation error.", e);
      safetyNetAttestationJwsResult = "ERROR:" + e.toString();
    }
*/

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
            .setSafetynetNonce(ByteString.copyFrom(safetyNetNonce))
            .build();

    String url = "hello/" + deviceRegistrationKey;
    ApiRequest request = new ApiRequest.Builder(url, "PUT").body(req).build();
    RequestManager.i.sendRequestSync(request);
    if (request.error() == null) {
      Messages.HelloResponse resp = request.body(Messages.HelloResponse.class);
      helloResponsePb = resp;
      if (resp == null) {
        scheduleRetry("Unexpected error", retries);
        return;
      }

      FeatureFlags.setup(helloResponsePb.getFeatureFlags());

      if (resp.hasEmpire()) {
        MyEmpire myEmpire = new MyEmpire();
        myEmpire.fromProtocolBuffer(resp.getEmpire());
        EmpireManager.i.setup(myEmpire);
      } else {
        tr.runTask(
            () -> activity.getNavController().navigate(R.id.empireSetupFragment), Threads.UI);
        return;
      }

      if (resp.hasWasEmpireReset() && resp.getWasEmpireReset()) {
        if (resp.hasEmpireResetReason() && resp.getEmpireResetReason().length() > 0) {
          String resetReason = resp.getEmpireResetReason();
          if (resetReason.equals("blitz")) {
            tr.runTask(
                () -> activity.getNavController().navigate(R.id.blitzResetFragment), Threads.UI);
            return;
          }
        }

        tr.runTask(
            () -> activity.getNavController().navigate(
                R.id.empireSetupFragment,
                new EmpireResetFragmentArgs.Builder(resp.getEmpireResetReason())
                    .build()
                    .toBundle()),
            Threads.UI);
        return;
      }

      if (resp.hasRequireGcmRegister() && resp.getRequireGcmRegister()) {
        log.info("Re-registering for GCM...");
        //GCMIntentService.register(activity);
        // we can keep going, though...
      }

      BuildManager.i.setup(resp.getBuildingStatistics());

      tr.runTask(() -> greetingWatcher.onComplete(resp.getServerVersion()), Threads.UI);
    } else {
      log.error("Error occurred in 'hello': %s", request.error().getErrorMessage());

      @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      Throwable exception = request.exception();
      if (exception != null) {
        // if there's no status message, it likely means we were unable to connect
        // (i.e. a network error) just keep retrying until it works.
        scheduleRetry("An error occurred talking to the server, check data connection.", retries);
      } else if (request.error().getErrorCode()
          == Messages.GenericError.ErrorCode.UpgradeRequired.getNumber()) {
        String msg = request.error().getErrorMessage().replace("\\n", "\n");
        tr.runTask(() -> greetingWatcher.onFailed(msg, GiveUpReason.UPGRADE_REQUIRED), Threads.UI);
      } else if (request.error().getErrorCode()
          == Messages.GenericError.ErrorCode.AuthenticationError.getNumber()) {
        // if it's an authentication problem, we'll want to re-authenticate
        // TODO: schedule re-auth
        tr.runTask(
            () -> greetingWatcher.onFailed("Authentication failed.", GiveUpReason.UNEXPECTED_ERROR),
            Threads.UI);
      } else if (request.error().getErrorCode()
          == Messages.GenericError.ErrorCode.ClientDeviceRejected.getNumber()) {
        // the client was rejected for some reason (e.g. auto-clicker installed, failed
        // SafetyNet validation, etc).
        tr.runTask(
            () -> greetingWatcher.onFailed(
                request.error().getErrorMessage(), GiveUpReason.CLIENT_REJECTED),
            Threads.UI);
      } else {
        // any other HTTP error, let's display that
        scheduleRetry("Unexpected error", retries);
      }
    }
  }

  private String getAccountName() {
    SharedPreferences prefs = Util.getSharedPreferences();
    String accountName = prefs.getString("AccountName", null);
    if (accountName == null) {
      // If we don't have an account name, just generate a random one that we'll be able to use
      // (though on this device only, and assuming you never clear your data!)
      accountName = generateAnonUserName();
      prefs.edit().putString("AccountName", accountName).apply();
    }

    return accountName;
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

    /**
     * Some kind of unexpected error (e.g. getting an 'authentication error' while anonymous -- this
     * should be impossible.
     */
    UNEXPECTED_ERROR,
  }
}
