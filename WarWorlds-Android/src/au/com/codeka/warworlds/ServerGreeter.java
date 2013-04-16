package au.com.codeka.warworlds;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.protobuf.Messages;

import com.google.android.gcm.GCMRegistrar;

/**
 * This class is used to make sure we're said "Hello" to the server and that we've got our
 * empire and stuff all set up.
 */
public class ServerGreeter {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private static ArrayList<HelloCompleteHandler> mHelloCompleteHandlers;
    private static ArrayList<HelloWatcher> mHelloWatchers;
    private static Handler mHandler;
    private static boolean mHelloStarted;
    private static boolean mHelloComplete;
    private static ServerGreeting mServerGreeting;

    static {
        mHelloCompleteHandlers = new ArrayList<HelloCompleteHandler>();
        mHelloWatchers = new ArrayList<HelloWatcher>();
        clearHello();
    }

    public static void addHelloWatcher(HelloWatcher watcher) {
        synchronized(mHelloWatchers) {
            mHelloWatchers.add(watcher);
        }
    }
    public static void removeHelloWatcher(HelloWatcher watcher) {
        synchronized(mHelloWatchers) {
            mHelloWatchers.remove(watcher);
        }
    }

    public static void clearHello() {
        mHelloStarted = false;
        mHelloComplete = false;
        mServerGreeting = new ServerGreeting();
    }

    public static void waitForHello(BaseActivity activity, HelloCompleteHandler handler) {
        if (mHelloComplete) {
            log.debug("Already said 'hello', not saying it again...");
            handler.onHelloComplete(true, mServerGreeting);
            return;
        }

        synchronized(mHelloCompleteHandlers) {
            mHelloCompleteHandlers.add(handler);

            if (!mHelloStarted) {
                mHelloStarted = true;
                sayHello(activity, 0);
            }
        }
    }

    private static void fireHelloComplete(boolean success) {
        synchronized(mHelloCompleteHandlers) {
            for (HelloCompleteHandler handler : mHelloCompleteHandlers) {
                handler.onHelloComplete(success, mServerGreeting);
            }
        }
    }

    private static void sayHello(final BaseActivity activity, final int retries) {
        log.debug("Saying 'hello'...");

        Util.loadProperties(activity);
        if (Util.isDebug()) {
            enableStrictMode();
        }

        GCMRegistrar.checkDevice(activity);
        GCMRegistrar.checkManifest(activity);

        Authenticator.configure(activity);
        PreferenceManager.setDefaultValues(activity, R.xml.global_options, false);

        int memoryClass = ((ActivityManager) activity.getSystemService(BaseActivity.ACTIVITY_SERVICE)).getMemoryClass();
        if (memoryClass < 40) {
            // on low memory devices, we want to make sure the background detail is always BLACK
            // this is a bit of a hack, but should stop the worst of the memory issues (I hope!)
            new GlobalOptions(activity).setStarfieldDetail(GlobalOptions.StarfieldDetail.BLACK);
        }

        // if we've saved off the authentication cookie, cool!
        SharedPreferences prefs = Util.getSharedPreferences(activity);
        final String accountName = prefs.getString("AccountName", null);
        if (accountName == null) {
            fireHelloComplete(false);
            activity.startActivity(new Intent(activity, AccountsActivity.class));
            return;
        }

        mServerGreeting.mIsConnected = false;
        if (mHandler == null) {
            mHandler = new Handler();
        }

        new BackgroundRunner<String>() {
            private boolean mNeedsEmpireSetup;
            private boolean mErrorOccured;
            private boolean mNeedsReAuthenticate;
            private boolean mWasEmpireReset;
            private ArrayList<Colony> mColonies;

            @Override
            protected String doInBackground() {
                // re-authenticate and get a new cookie
                String authCookie = Authenticator.authenticate(activity, accountName);
                ApiClient.getCookies().clear();
                ApiClient.getCookies().add(authCookie);
                log.debug("Got auth cookie: "+authCookie);

                // Schedule registration with GCM, which will update our device
                // when we get the registration ID
                GCMIntentService.register(activity);
                String deviceRegistrationKey = DeviceRegistrar.getDeviceRegistrationKey(activity);
                if (deviceRegistrationKey == null || deviceRegistrationKey.length() == 0) {
                    deviceRegistrationKey = DeviceRegistrar.register(activity);
                }

                // say hello to the server
                String message;
                try {
                    int memoryClass = ((ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE)).getMemoryClass();
                    Messages.HelloRequest req = Messages.HelloRequest.newBuilder()
                            .setDeviceBuild(android.os.Build.DISPLAY)
                            .setDeviceManufacturer(android.os.Build.MANUFACTURER)
                            .setDeviceModel(android.os.Build.MODEL)
                            .setDeviceVersion(android.os.Build.VERSION.RELEASE)
                            .setMemoryClass(memoryClass)
                            .build();

                    String url = "hello/"+deviceRegistrationKey;
                    Messages.HelloResponse resp = ApiClient.putProtoBuf(url, req, Messages.HelloResponse.class);
                    if (resp.hasEmpire()) {
                        mNeedsEmpireSetup = false;
                        EmpireManager.getInstance().setup(
                                MyEmpire.fromProtocolBuffer(resp.getEmpire()));
                    } else {
                        mNeedsEmpireSetup = true;
                    }

                    if (resp.hasWasEmpireReset() && resp.getWasEmpireReset()) {
                        mWasEmpireReset = true;
                    }

                    if (resp.hasRequireGcmRegister() && resp.getRequireGcmRegister()) {
                        log.info("Re-registering for GCM...");
                        GCMIntentService.register(activity);
                        // we can keep going, though...
                    }

                    mColonies = new ArrayList<Colony>();
                    for (Messages.Colony c : resp.getColoniesList()) {
                        if (c.getPopulation() < 1.0) {
                            continue;
                        }
                        mColonies.add(Colony.fromProtocolBuffer(c));
                    }

                    BuildManager.getInstance().setup(resp.getBuildingStatistics(), resp.getBuildRequestsList());

                    message = resp.getMotd().getMessage();
                    mErrorOccured = false;
                } catch(ApiException e) {
                    log.error("Error occurred in 'hello'", e);

                    if (e.getHttpStatusLine() == null) {
                        // if there's no status line, it likely means we were unable to connect
                        // (i.e. a network error) just keep retrying until it works.
                        message = "<p class=\"error\">An error occured talking to the server, check " +
                                "data connection.</p>";
                        mErrorOccured = true;
                        mNeedsReAuthenticate = false;
                    } else {
                        // an HTTP error is likely because our credentials are out of date, we'll
                        // want to re-authenticate ourselves.
                        message = "<p class=\"error\">Authentication failed.</p>";
                        mErrorOccured = true;
                        mNeedsReAuthenticate = true;
                    }
                }

                return message;
            }

            @Override
            protected void onComplete(String result) {
                mServerGreeting.mIsConnected = true;

                if (mNeedsEmpireSetup) {
                    mServerGreeting.mIntent = new Intent(activity, EmpireSetupActivity.class);
                    mHelloComplete = true;
                } else if (!mErrorOccured) {
                    Util.setup(activity);
                    ChatManager.getInstance().setup(activity);

                    // make sure we're correctly registered as online.
                    BackgroundDetector.getInstance().onBackgroundStatusChange(activity);

                    mServerGreeting.mMessageOfTheDay = result;
                    mServerGreeting.mColonies = mColonies;
                    mHelloComplete = true;
                } else /* mErrorOccured */ {
                    mServerGreeting.mIsConnected = false;

                    if (mNeedsReAuthenticate) {
                        // if we need to re-authenticate, first forget the current credentials
                        // the switch to the AccountsActivity.
                        final SharedPreferences prefs = Util.getSharedPreferences(activity);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove("AccountName");
                        editor.commit();

                        mServerGreeting.mIntent = new Intent(activity, AccountsActivity.class);
                        mHelloComplete = true;
                    } else {
                        synchronized(mHelloWatchers) {
                            for (HelloWatcher watcher : mHelloWatchers) {
                                watcher.onRetry(retries + 1);
                            }
                        }

                        // otherwise, just try again
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sayHello(activity, retries + 1);
                            }
                        }, 3000);
                        mHelloComplete = false;
                    }
                }

                if (mWasEmpireReset) {
                    mServerGreeting.mIntent = new Intent(activity, EmpireResetActivity.class);
                }

                if (mHelloComplete) {
                    synchronized(mHelloCompleteHandlers) {
                        for (HelloCompleteHandler handler : mHelloCompleteHandlers) {
                            handler.onHelloComplete(!mErrorOccured, mServerGreeting);
                        }
                        mHelloCompleteHandlers = new ArrayList<HelloCompleteHandler>();
                    }

                    if (mServerGreeting.mIntent != null) {
                        activity.startActivity(mServerGreeting.mIntent);
                    }
                }
            }
        }.execute();
    }

    @SuppressLint({ "NewApi" }) // StrictMode doesn't work on < 3.0
    private static void enableStrictMode() {
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                      .detectDiskReads()
                      .detectDiskWrites()
                      .detectNetwork()
                      .penaltyLog()
                      .build());
        } catch(Exception e) {
            // ignore errors
        }
    }

    public static class ServerGreeting {
        private boolean mIsConnected;
        private String mMessageOfTheDay;
        private Intent mIntent;
        private ArrayList<Colony> mColonies;

        public boolean isConnected() {
            return mIsConnected;
        }

        public String getMessageOfTheDay() {
            return mMessageOfTheDay;
        }

        public Intent getIntent() {
            return mIntent;
        }

        public ArrayList<Colony> getColonies() {
            return mColonies;
        }
    }

    public interface HelloCompleteHandler {
        void onHelloComplete(boolean success, ServerGreeting greeting);
    }

    public interface HelloWatcher {
        void onRetry(int retries);
    }
}
