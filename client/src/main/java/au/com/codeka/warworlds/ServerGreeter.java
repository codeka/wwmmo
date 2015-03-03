package au.com.codeka.warworlds;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

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

import au.com.codeka.BackgroundRunner;
import au.com.codeka.ErrorReporter;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.ctrl.BannerAdView;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.auth.UserRecoverableAuthException;

/**
 * This class is used to make sure we're said "Hello" to the server and that we've got our
 * empire and stuff all set up.
 */
public class ServerGreeter {
  private static Log log = new Log("ServerGreeter");
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

  private static void fireHelloComplete(boolean success) {
    synchronized (helloCompleteHandlers) {
      for (HelloCompleteHandler handler : helloCompleteHandlers) {
        handler.onHelloComplete(success, serverGreeting);
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

    int memoryClass = ((ActivityManager) activity.getSystemService(BaseActivity.ACTIVITY_SERVICE))
        .getMemoryClass();
    if (memoryClass < 40) {
      // on low memory devices, we want to make sure the background detail is always BLACK
      // this is a bit of a hack, but should stop the worst of the memory issues (I hope!)
      new GlobalOptions().setStarfieldDetail(GlobalOptions.StarfieldDetail.BLACK);
    }

    // if we've saved off the authentication cookie, cool!
    SharedPreferences prefs = Util.getSharedPreferences();
    final String accountName = prefs.getString("AccountName", null);
    if (accountName == null) {
      fireHelloComplete(false);
      activity.startActivity(new Intent(activity, AccountsActivity.class));
      return;
    }

    serverGreeting.mIsConnected = false;
    if (handler == null) {
      handler = new Handler();
    }

    RequestManager.i.setup(activity);

    new BackgroundRunner<String>() {
      private boolean mNeedsEmpireSetup;
      private boolean mErrorOccured;
      private boolean mNeedsReAuthenticate;
      private boolean mWasEmpireReset;
      private String mResetReason;
      private String mToastMessage;
      private Intent mIntent;

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
            mErrorOccured = true;
            // if it wasn't a network error, it probably means we need to re-auth.
            mNeedsReAuthenticate = !e.networkError();
            if (e.getCause() instanceof UserRecoverableAuthException) {
              mIntent = ((UserRecoverableAuthException) e.getCause()).getIntent();
            }
            if (e.getServerErrorCode() > 0 && e.getServerErrorMessage() != null) {
              mToastMessage = e.getServerErrorMessage();
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
            mErrorOccured = true;
            // only re-authenticate for non-network related errors
            mNeedsReAuthenticate = !e.networkError();
            if (e.getServerErrorCode() > 0 && e.getServerErrorMessage() != null) {
              mToastMessage = e.getServerErrorMessage();
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
        try {
          int memoryClass = ((ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE))
              .getMemoryClass();
          Messages.HelloRequest req =
              Messages.HelloRequest.newBuilder().setDeviceBuild(android.os.Build.DISPLAY)
                  .setDeviceManufacturer(android.os.Build.MANUFACTURER)
                  .setDeviceModel(android.os.Build.MODEL)
                  .setDeviceVersion(android.os.Build.VERSION.RELEASE).setMemoryClass(memoryClass)
                  .setAllowInlineNotfications(false).setNoStarList(true).build();

          String url = "hello/" + deviceRegistrationKey;
          Messages.HelloResponse resp =
              ApiClient.putProtoBuf(url, req, Messages.HelloResponse.class);
          log.info("GOT RESPONSE FROM HELLO REQUEST!");
          if (resp.hasEmpire()) {
            mNeedsEmpireSetup = false;
            MyEmpire myEmpire = new MyEmpire();
            myEmpire.fromProtocolBuffer(resp.getEmpire());
            EmpireManager.i.setup(myEmpire);
          } else {
            mNeedsEmpireSetup = true;
          }

          if (resp.hasWasEmpireReset() && resp.getWasEmpireReset()) {
            mWasEmpireReset = true;
            if (resp.hasEmpireResetReason() && resp.getEmpireResetReason().length() > 0) {
              mResetReason = resp.getEmpireResetReason();
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
          mErrorOccured = false;
        } catch (ApiException e) {
          log.error("Error occurred in 'hello'", e);

          if (e.getHttpStatusMessage() == null) {
            // if there's no status message, it likely means we were unable to connect
            // (i.e. a network error) just keep retrying until it works.
            message = "<p class=\"error\">An error occured talking to the server, check "
                + "data connection.</p>";
            mErrorOccured = true;
            mNeedsReAuthenticate = false;
          } else if (e.getCause() instanceof UserRecoverableAuthException) {
            message = "<p class=\"error\">Authentication failed.</p>";
            mErrorOccured = true;
            mNeedsReAuthenticate = true;
            mIntent = ((UserRecoverableAuthException) e.getCause()).getIntent();
          } else if (e.getHttpStatusCode() == 403) {
            // if it's an authentication problem, we'll want to re-authenticate
            message = "<p class=\"error\">Authentication failed.</p>";
            mErrorOccured = true;
            mNeedsReAuthenticate = true;
          } else {
            // any other HTTP error, let's display that
            message = "<p class=\"error\">AN ERROR OCCURED.</p>";
            mErrorOccured = true;
            mNeedsReAuthenticate = false;
          }
        }

        return message;
      }

      @Override
      protected void onComplete(String result) {
        serverGreeting.mIsConnected = true;

        if (mNeedsEmpireSetup) {
          serverGreeting.mIntent = new Intent(activity, EmpireSetupActivity.class);
          helloComplete = true;
        } else if (!mErrorOccured) {
          Util.setup(activity);
          ChatManager.i.setup();
          Notifications.startLongPoll();

          // make sure we're correctly registered as online.
          BackgroundDetector.i.onBackgroundStatusChange();

          serverGreeting.mMessageOfTheDay = result;
          helloComplete = true;
        } else /* mErrorOccured */ {
          serverGreeting.mIsConnected = false;

          if (mToastMessage != null && mToastMessage.length() > 0) {
            Toast toast = Toast.makeText(App.i, mToastMessage, Toast.LENGTH_LONG);
            toast.show();
          }

          if (mNeedsReAuthenticate) {
            // if we need to re-authenticate, first forget the current credentials
            // the switch to the AccountsActivity.
            final SharedPreferences prefs = Util.getSharedPreferences();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("AccountName");
            editor.apply();

            if (mIntent != null) {
              serverGreeting.mIntent = mIntent;
            } else {
              serverGreeting.mIntent = new Intent(activity, AccountsActivity.class);
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

        if (mWasEmpireReset) {
          if (mResetReason != null && mResetReason.equals("blitz")) {
            serverGreeting.mIntent = new Intent(activity, BlitzResetActivity.class);
          } else {
            serverGreeting.mIntent = new Intent(activity, EmpireResetActivity.class);
            if (mResetReason != null) {
              serverGreeting.mIntent.putExtra("au.com.codeka.warworlds.ResetReason", mResetReason);
            }
          }
        }

        if (helloComplete) {
          synchronized (helloCompleteHandlers) {
            for (HelloCompleteHandler handler : helloCompleteHandlers) {
              handler.onHelloComplete(!mErrorOccured, serverGreeting);
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

  public static class ServerGreeting {
    private boolean mIsConnected;
    private String mMessageOfTheDay;
    private Intent mIntent;

    public boolean isConnected() {
      return mIsConnected;
    }

    public String getMessageOfTheDay() {
      return mMessageOfTheDay;
    }

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
