package au.com.codeka.warworlds;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.common.protobuf.Messages;

/**
 * This class is used to detect when the app goes into the background. Basically
 * we have to make sure to inherit from BaseActivity in all activities.
 * 
 * The algorithm used to detect when we go to the background is described in this
 * Stack Overflow article: http://stackoverflow.com/questions/4414171/
 * Basically:
 * 
 * * in onResume, increment the number of running activities
 * * in onPause, decrement the number of running activities
 * * If the number of running activities hits zero, you're going into the
 *   background, UNLESS a new activity from the same package is being started.
 * 
 * To detect when a new activity from the same package is being started, we
 * need to override startActivity() and save the package of the starting
 * activity. In onPostResume(), we clear it again. If there's a saved package
 * name when the activity count hits zero, then we're not going into the
 * background.
 */
public class BackgroundDetector {
    private static BackgroundDetector sInstance = new BackgroundDetector();
    public static BackgroundDetector getInstance() {
        return sInstance;
    }
    private BackgroundDetector() {
        mIsInBackground = true;
        mIsTransitioningToBackground = false;
        mBackgroundChangeHandlers = new ArrayList<BackgroundChangeHandler>();
        mHandler = new Handler();
    }
    private static Logger log = LoggerFactory.getLogger(BackgroundDetector.class);

    private int mNumActiveActivities;
    private String mStartingActivityPackage;
    private boolean mIsInBackground;
    private ArrayList<BackgroundChangeHandler> mBackgroundChangeHandlers;
    private boolean mIsTransitioningToBackground;
    private Handler mHandler;

    public boolean isInBackground() {
        return mIsInBackground;
    }

    public void addBackgroundChangeHandler(BackgroundChangeHandler handler) {
        synchronized(mBackgroundChangeHandlers) {
            mBackgroundChangeHandlers.add(handler);
        }
    }
    public void removeBackgroundChangeHandler(BackgroundChangeHandler handler) {
        synchronized(mBackgroundChangeHandlers) {
            mBackgroundChangeHandlers.remove(handler);
        }
    }

    private void fireBackgroundChangeHandlers(Context context) {
        synchronized(mBackgroundChangeHandlers) {
            for (BackgroundChangeHandler handler : mBackgroundChangeHandlers) {
                handler.onBackgroundChange(context, mIsInBackground);
            }
        }
    }

    public void onBackgroundStatusChange(final BaseActivity activity) {
        if (!Util.isSetup() || RealmContext.i.getCurrentRealm() == null) {
            return;
        }

        final Messages.DeviceOnlineStatus dos_pb =
                Messages.DeviceOnlineStatus.newBuilder()
                                           .setIsOnline(!mIsInBackground)
                                           .build();

        final String deviceRegistrationKey = DeviceRegistrar.getDeviceRegistrationKey();
        if (deviceRegistrationKey == null || deviceRegistrationKey.length() == 0) {
            return;
        }

        if (!ServerGreeter.isHelloComplete()) {
            // if we haven't said 'hello' to the server yet, no point continuing.
            return;
        }

        new BackgroundRunner<Void>() {
            @Override
            protected Void doInBackground() {
                String url = "devices/"+deviceRegistrationKey+"?online_status=1";
                try {
                    ApiClient.putProtoBuf(url, dos_pb);
                } catch (ApiException e) {
                    log.error("Could not update online status, ignored.");
                }

                return null;
            }

            @Override
            protected void onComplete(Void arg) {
            }
        }.execute();

        fireBackgroundChangeHandlers(activity);
    }

    private void transitionToBackground(final BaseActivity activity) {
        if (mIsTransitioningToBackground) {
            return;
        }

        mIsTransitioningToBackground = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsTransitioningToBackground) {
                    mIsTransitioningToBackground = false;
                    mIsInBackground = true;
                    onBackgroundStatusChange(activity);
                }
            }
        }, 5000);
    }

    private void transitionToForeground(BaseActivity activity) {
        if (mIsTransitioningToBackground) {
            mIsTransitioningToBackground = false;
            return;
        }

        mIsInBackground = false;
        onBackgroundStatusChange(activity);
    }

    public void onActivityPause(BaseActivity activity) {
        mNumActiveActivities --;
        if (mNumActiveActivities <= 0) {
            if (mStartingActivityPackage != null &&
                mStartingActivityPackage.startsWith("au.com.codeka.warworlds")) {
            } else {
                transitionToBackground(activity);
            }
        }
    }

    public void onActivityResume(BaseActivity activity) {
        mNumActiveActivities ++;
        if (mNumActiveActivities == 1) {
            if (mStartingActivityPackage != null &&
                mStartingActivityPackage.startsWith("au.com.codeka.warworlds")) {
            } else {
                transitionToForeground(activity);
            }
        }
    }

    public void onStartActivity(BaseActivity callingActivity, Intent intent) {
        mStartingActivityPackage = null;

        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            mStartingActivityPackage = componentName.getPackageName();
        }

        log.info("Starting activity: "+mStartingActivityPackage);
    }

    public void onActivityPostResume(BaseActivity activity) {
        log.info("Activity started.");
        mStartingActivityPackage = null;
    }

    public interface BackgroundChangeHandler {
        public void onBackgroundChange(Context context, boolean isInBackground);
    }
}
