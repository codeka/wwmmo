package au.com.codeka.warworlds;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.protobuf.Messages;

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
        mBackgroundChangeHandlers = new ArrayList<BackgroundChangeHandler>();
    }
    private static Logger log = LoggerFactory.getLogger(BackgroundDetector.class);

    private int mNumActiveActivities;
    private String mStartingActivityPackage;
    private boolean mIsInBackground;
    private ArrayList<BackgroundChangeHandler> mBackgroundChangeHandlers;

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

    private void fireBackgroundChangeHandlers() {
        synchronized(mBackgroundChangeHandlers) {
            for (BackgroundChangeHandler handler : mBackgroundChangeHandlers) {
                handler.onBackgroundChange(mIsInBackground);
            }
        }
    }

    public void onBackgroundStatusChange(final BaseActivity activity) {
        final Messages.DeviceOnlineStatus dos_pb =
                Messages.DeviceOnlineStatus.newBuilder()
                                           .setIsOnline(!mIsInBackground)
                                           .build();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String url = "/api/v1/devices/"+DeviceRegistrar.getDeviceRegistrationKey(activity);
                try {
                    ApiClient.putProtoBuf(url, dos_pb);
                } catch (ApiException e) {
                    log.error("Could not update online status, ignored.");
                }

                return null;
            }
        }.execute();

        fireBackgroundChangeHandlers();
    }

    public void onActivityPause(BaseActivity activity) {
        mNumActiveActivities --;
        if (mNumActiveActivities <= 0) {
            if (mStartingActivityPackage != null &&
                mStartingActivityPackage.startsWith("au.com.codeka.warworlds")) {
            } else {
                mIsInBackground = true;
                onBackgroundStatusChange(activity);
            }
        }
    }

    public void onActivityResume(BaseActivity activity) {
        mNumActiveActivities ++;
        if (mNumActiveActivities == 1) {
            if (mStartingActivityPackage != null &&
                mStartingActivityPackage.startsWith("au.com.codeka.warworlds")) {
            } else {
                mIsInBackground = false;
                onBackgroundStatusChange(activity);
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
        public void onBackgroundChange(boolean isInBackground);
    }
}
