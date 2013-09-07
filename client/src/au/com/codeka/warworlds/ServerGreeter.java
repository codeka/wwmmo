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
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.HelloRequest;
import au.com.codeka.common.model.HelloResponse;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.BannerAdView;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;

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

    /**
     * When we change realms, we'll want to make sure we say 'hello' again.
     */
    private static RealmManager.RealmChangedHandler mRealmChangedHandler = new RealmManager.RealmChangedHandler() {
        @Override
        public void onRealmChanged(Realm newRealm) {
            clearHello();
        }
    };

    static {
        mHelloCompleteHandlers = new ArrayList<HelloCompleteHandler>();
        mHelloWatchers = new ArrayList<HelloWatcher>();
        clearHello();
        RealmManager.i.addRealmChangedHandler(mRealmChangedHandler);
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

    public static boolean isHelloComplete() {
        return mHelloComplete;
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
        Util.setup(activity);

        Util.loadProperties();
        if (Util.isDebug()) {
            enableStrictMode();
        }

        GCMRegistrar.checkDevice(activity);
        GCMRegistrar.checkManifest(activity);

        PreferenceManager.setDefaultValues(activity, R.xml.global_options, false);

        int memoryClass = ((ActivityManager) activity.getSystemService(BaseActivity.ACTIVITY_SERVICE)).getMemoryClass();
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

        mServerGreeting.mIsConnected = false;
        if (mHandler == null) {
            mHandler = new Handler();
        }

        new BackgroundRunner<String>() {
            private boolean mNeedsEmpireSetup;
            private boolean mErrorOccured;
            private boolean mNeedsReAuthenticate;
            private boolean mWasEmpireReset;
            private String mResetReason;
            private ArrayList<Colony> mColonies;
            private ArrayList<Long> mStarIDs;

            @Override
            protected String doInBackground() {
                Realm realm = RealmContext.i.getCurrentRealm();
                if (!realm.getAuthenticator().isAuthenticated()) {
                    try {
                        realm.getAuthenticator().authenticate(activity, realm);
                    } catch (ApiException e) {
                    }
                }

                // Schedule registration with GCM, which will update our device
                // when we get the registration ID
                GCMIntentService.register(activity);
                String deviceRegistrationKey = DeviceRegistrar.getDeviceRegistrationKey();
                if (deviceRegistrationKey == null || deviceRegistrationKey.length() == 0) {
                    deviceRegistrationKey = DeviceRegistrar.register();
                }

                // say hello to the server
                String message;
                try {
                    int memoryClass = ((ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE)).getMemoryClass();
                    HelloRequest req = new HelloRequest.Builder()
                            .device_build(android.os.Build.DISPLAY)
                            .device_manufacturer(android.os.Build.MANUFACTURER)
                            .device_model(android.os.Build.MODEL)
                            .device_version(android.os.Build.VERSION.RELEASE)
                            .memory_class(memoryClass)
                            .allow_inline_notfications(true)
                            .build();

                    String url = "hello/"+deviceRegistrationKey;
                    HelloResponse resp = ApiClient.putProtoBuf(url, req, HelloResponse.class);
                    if (resp.empire != null) {
                        mNeedsEmpireSetup = false;
                        EmpireManager.i.setup(resp.empire);
                    } else {
                        mNeedsEmpireSetup = true;
                    }

                    if (resp.was_empire_reset != null && resp.was_empire_reset) {
                        mWasEmpireReset = true;
                        if (resp.empire_reset_reason != null) {
                            mResetReason = resp.empire_reset_reason;
                        }
                    }

                    if (resp.force_remove_ads != null && resp.force_remove_ads) {
                        BannerAdView.removeAds();
                    }

                    if (resp.require_gcm_register != null && resp.require_gcm_register) {
                        log.info("Re-registering for GCM...");
                        GCMIntentService.register(activity);
                        // we can keep going, though...
                    }

                    mColonies = new ArrayList<Colony>();
                    for (Colony c : resp.colonies) {
                        if (c.population < 1.0) {
                            continue;
                        }
                        mColonies.add(c);
                    }

                    mStarIDs = new ArrayList<Long>(resp.star_ids);

                    BuildManager.getInstance().setup(resp.building_statistics, resp.build_requests);

                    message = resp.motd.message;
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
                mServerGreeting.mStarIDs = mStarIDs;

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
                        final SharedPreferences prefs = Util.getSharedPreferences();
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
                    if (mResetReason != null && mResetReason.equals("blitz")) {
                        mServerGreeting.mIntent = new Intent(activity, BlitzResetActivity.class);
                    } else {
                        mServerGreeting.mIntent = new Intent(activity, EmpireResetActivity.class);
                        if (mResetReason != null) {
                            mServerGreeting.mIntent.putExtra("au.com.codeka.warworlds.ResetReason", mResetReason);
                        }
                    }
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
        private ArrayList<Long> mStarIDs;

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

        public ArrayList<Long> getStarIDs() {
            return mStarIDs;
        }
    }

    public interface HelloCompleteHandler {
        void onHelloComplete(boolean success, ServerGreeting greeting);
    }

    public interface HelloWatcher {
        void onRetry(int retries);
    }
}
