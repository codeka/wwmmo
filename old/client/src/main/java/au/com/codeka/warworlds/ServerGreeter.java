package au.com.codeka.warworlds;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.ArrayList;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.ErrorReporter;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.ctrl.BannerAdView;
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
  private static Handler handler;
  private static boolean helloStarted;
  private static boolean helloComplete;
  private static ServerGreeting serverGreeting;

  /**
   * When we change realms, we'll want to make sure we say 'hello' again.
   */
  private static final RealmManager.RealmChangedHandler realmChangedHandler =
      new RealmManager.RealmChangedHandler() {
        @Override
        public void onRealmChanged(Realm newRealm) {
          clearHello();
        }
      };

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
  }

  public static void waitForHello(Activity activity, HelloCompleteHandler handler) {
    if (helloComplete) {
      log.debug("Already said 'hello', not saying it again...");
      handler.onHelloComplete(true, serverGreeting);
      return;
    }

    synchronized (helloCompleteHandlers) {
      helloCompleteHandlers.add(handler);

      if (!helloStarted) {
        helloStarted = true;
        sayHello(activity, 0);
      }
    }
  }

  private static void sayHello(final Activity activity, final int retries) {
    LogImpl.setup();
    log.debug("Saying 'hello'...");
    Util.setup(activity);

    Util.loadProperties();
    if (Util.isDebug()) {
      enableStrictMode();
    }

    GCMRegistrar.checkDevice(activity);
    GCMRegistrar.checkManifest(activity);

    PreferenceManager.setDefaultValues(activity, R.xml.global_options, false);

    int memoryClass = ((ActivityManager) activity
        .getSystemService(BaseActivity.ACTIVITY_SERVICE)).getMemoryClass();
    if (memoryClass < 40) {
      // on low memory devices, we want to make sure the background detail is always BLACK
      // this is a bit of a hack, but should stop the worst of the memory issues (I hope!)
      new GlobalOptions().setStarfieldDetail(GlobalOptions.StarfieldDetail.BLACK);
    }

    // if we've saved off the authentication cookie, cool!
    SharedPreferences prefs = Util.getSharedPreferences();
    String accountName = prefs.getString("AccountName", null);
    if (accountName == null) {
      // If we don't have an account name, just generate a random one that we'll be able to use
      // (though on this device only, and assuming you never clear your data!)
      accountName = generateAnonUserName();
      prefs.edit().putString("AccountName", accountName).apply();
    }

    if (handler == null) {
      handler = new Handler();
    }

    RequestManager.i.setup(activity);

    new BackgroundRunner<String>() {
      private boolean needsEmpireSetup;
      private boolean errorOccurred;
      private boolean needsReAuthenticate;
      private boolean wasEmpireReset;
      private String resetReason;
      private String toastMessage;
      private Intent intent;

      @Override
      protected String doInBackground() {
        handler.post(new Runnable() {
          @Override
          public void run() {
            synchronized (helloWatchers) {
              for (HelloWatcher watcher : helloWatchers) {
                watcher.onAuthenticating();
              }
            }
          }
        });

        Realm realm = RealmContext.i.getCurrentRealm();
        if (!realm.getAuthenticator().isAuthenticated()) {
          try {
            log.info("Not authenticated, re-authenticating.");
            realm.getAuthenticator().authenticate(activity, realm);
          } catch (ApiException e) {
            errorOccurred = true;
            // if it wasn't a network error, it probably means we need to re-auth.
            needsReAuthenticate = !e.networkError();
            if (e.getCause() instanceof UserRecoverableAuthException) {
              intent = ((UserRecoverableAuthException) e.getCause()).getIntent();
            }
            if (e.getServerErrorCode() > 0 && e.getServerErrorMessage() != null) {
              toastMessage = e.getServerErrorMessage();
            }
            return null;
          }
        }

        // Schedule registration with GCM, which will update our device
        // when we get the registration ID
        GCMIntentService.register(activity);
        String deviceRegistrationKey = DeviceRegistrar.getDeviceRegistrationKey();
        if (deviceRegistrationKey == null || deviceRegistrationKey.length() == 0) {
          try {
            deviceRegistrationKey = DeviceRegistrar.register();
          } catch (ApiException e) {
            errorOccurred = true;
            // only re-authenticate for non-network related errors
            needsReAuthenticate = !e.networkError();
            if (e.getServerErrorCode() > 0 && e.getServerErrorMessage() != null) {
              toastMessage = e.getServerErrorMessage();
            }
            return null;
          }
        }

        handler.post(new Runnable() {
          @Override
          public void run() {
            synchronized (helloWatchers) {
              for (HelloWatcher watcher : helloWatchers) {
                watcher.onConnecting();
              }
            }
          }
        });

        // say hello to the server
        String message;
        int memoryClass = ((ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE))
            .getMemoryClass();
        Messages.HelloRequest req =
            Messages.HelloRequest.newBuilder().setDeviceBuild(android.os.Build.DISPLAY)
                .setDeviceManufacturer(android.os.Build.MANUFACTURER)
                .setDeviceModel(android.os.Build.MODEL)
                .setDeviceVersion(android.os.Build.VERSION.RELEASE).setMemoryClass(memoryClass)
                .setAllowInlineNotfications(false).setNoStarList(true).build();

        String url = "hello/" + deviceRegistrationKey;
        ApiRequest request = new ApiRequest.Builder(url, "PUT").body(req).build();
        RequestManager.i.sendRequestSync(request);
        if (request.error() == null) {
          Messages.HelloResponse resp = request.body(Messages.HelloResponse.class);
          if (resp == null) {
            errorOccurred = true;
            return "Unknown error";
          }

          if (resp.hasEmpire()) {
            needsEmpireSetup = false;
            MyEmpire myEmpire = new MyEmpire();
            myEmpire.fromProtocolBuffer(resp.getEmpire());
            EmpireManager.i.setup(myEmpire);
          } else {
            needsEmpireSetup = true;
          }

          if (resp.hasWasEmpireReset() && resp.getWasEmpireReset()) {
            wasEmpireReset = true;
            if (resp.hasEmpireResetReason() && resp.getEmpireResetReason().length() > 0) {
              resetReason = resp.getEmpireResetReason();
            }
          }

          if (resp.hasForceRemoveAds() && resp.getForceRemoveAds()) {
            BannerAdView.removeAds();
          }

          if (resp.hasRequireGcmRegister() && resp.getRequireGcmRegister()) {
            log.info("Re-registering for GCM...");
            GCMIntentService.register(activity);
            // we can keep going, though...
          }

          BuildManager.i.setup(resp.getBuildingStatistics());

          message = resp.getMotd().getMessage();
          errorOccurred = false;
        } else {
          log.error("Error occurred in 'hello': %s", request.error().getErrorMessage());

          @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
          Throwable exception = request.exception();
          if (exception != null
                && (exception.getCause() instanceof UserRecoverableAuthException)) {
              message = "<p class=\"error\">Authentication failed.</p>";
              errorOccurred = true;
              needsReAuthenticate = true;
              intent = ((UserRecoverableAuthException) exception.getCause()).getIntent();
          } else if (exception != null) {
            // if there's no status message, it likely means we were unable to connect
            // (i.e. a network error) just keep retrying until it works.
            message = "<p class=\"error\">An error occurred talking to the server, check "
                + "data connection.</p>";
            errorOccurred = true;
            needsReAuthenticate = false;
          } else if (request.error().getErrorCode()
              == Messages.GenericError.ErrorCode.AuthenticationError.getNumber()) {
            // if it's an authentication problem, we'll want to re-authenticate
            message = "<p class=\"error\">Authentication failed.</p>";
            errorOccurred = true;
            needsReAuthenticate = true;
          } else {
            // any other HTTP error, let's display that
            message = "<p class=\"error\">AN ERROR OCCURRED.</p>";
            errorOccurred = true;
            needsReAuthenticate = false;
          }
        }

        return message;
      }

      @Override
      protected void onComplete(String result) {
        if (needsEmpireSetup) {
          serverGreeting.mIntent = new Intent(activity, EmpireSetupActivity.class);
          helloComplete = true;
        } else if (!errorOccurred) {
          Util.setup(activity);
          ChatManager.i.setup();
          Notifications.startLongPoll();

          // make sure we're correctly registered as online.
          BackgroundDetector.i.onBackgroundStatusChange();

          helloComplete = true;
        } else /* errorOccurred */ {
          if (toastMessage != null && toastMessage.length() > 0) {
            Toast toast = Toast.makeText(App.i, toastMessage, Toast.LENGTH_LONG);
            toast.show();
          }

          if (needsReAuthenticate) {
            // if we need to re-authenticate, first forget the current credentials
            // the switch to the AccountsActivity.
            final SharedPreferences prefs = Util.getSharedPreferences();
            String currAccountName = prefs.getString("AccountName", null);
            if (currAccountName != null && currAccountName.endsWith("@anon.war-worlds.com")) {
              log.error("Need to re-authenticate, but AccountName is anonymous, cannot re-auth.");
            } else {
              SharedPreferences.Editor editor = prefs.edit();
              editor.remove("AccountName");
              editor.apply();

              if (intent != null) {
                serverGreeting.mIntent = intent;
              } else {
                serverGreeting.mIntent = new Intent(activity, AccountsActivity.class);
              }
            }
            helloComplete = true;
          } else {
            synchronized (helloWatchers) {
              for (HelloWatcher watcher : helloWatchers) {
                watcher.onRetry(retries + 1);
              }
            }

            // otherwise, just try again
            handler.postDelayed(new Runnable() {
              @Override
              public void run() {
                sayHello(activity, retries + 1);
              }
            }, 3000);
            helloComplete = false;
          }
        }

        if (wasEmpireReset) {
          if (resetReason != null && resetReason.equals("blitz")) {
            serverGreeting.mIntent = new Intent(activity, BlitzResetActivity.class);
          } else {
            serverGreeting.mIntent = new Intent(activity, EmpireResetActivity.class);
            if (resetReason != null) {
              serverGreeting.mIntent.putExtra("au.com.codeka.warworlds.ResetReason", resetReason);
            }
          }
        }

        if (helloComplete) {
          synchronized (helloCompleteHandlers) {
            for (HelloCompleteHandler handler : helloCompleteHandlers) {
              handler.onHelloComplete(!errorOccurred, serverGreeting);
            }
            helloCompleteHandlers.clear();
          }

          if (serverGreeting.mIntent != null) {
            activity.startActivity(serverGreeting.mIntent);
          }

          ErrorReporter.register(activity);
        }
      }
    }.execute();
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
    private Intent mIntent;

    public Intent getIntent() {
      return mIntent;
    }
  }

  public interface HelloCompleteHandler {
    void onHelloComplete(boolean success, ServerGreeting greeting);
  }

  public interface HelloWatcher {
    void onAuthenticating();

    void onConnecting();

    void onRetry(int retries);
  }
}
