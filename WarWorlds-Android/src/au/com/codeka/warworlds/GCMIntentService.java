
package au.com.codeka.warworlds;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/**
 * Receive a push message from the Cloud to Device Messaging (C2DM) service.
 */
public class GCMIntentService extends GCMBaseIntentService {
    private static Logger log = LoggerFactory.getLogger(GCMIntentService.class);
    private static Callable<Void> sOnComplete;
    private static Activity sActivity;

    public static String PROJECT_ID = "990931198580";

    /**
     * Registers for C2DM notifications. Calls
     * AccountsActivity.registrationComplete() when finished.
     */
    public static void register(Activity activity, final Callable<Void> onComplete) {
        sOnComplete = onComplete;
        sActivity = activity;

        GCMRegistrar.register(activity, PROJECT_ID);
    }

    /**
     * Unregisters ourselves from C2DM notifications.
     */
    public static void unregister(Activity activity,
            final Callable<Void> onComplete) {
        sOnComplete = onComplete;
        sActivity = activity;
        GCMRegistrar.unregister(activity);
    }

    /**
     * Calls the onComplete handler (if there is one), making sure to do so on
     * the main UI thread.
     */
    private static void callOnComplete() {
        if (sOnComplete != null && sActivity != null) {
            sActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sOnComplete.call();
                        sOnComplete = null;
                    } catch (Exception e) {
                    }
                }
            });
        }
    }

    /**
     * Called when a registration token has been received.
     * 
     * @param context
     *            the Context
     * @param registrationId
     *            the registration id as a String
     * @throws IOException
     *             if registration cannot be performed
     */
    @Override
    public void onRegistered(Context context, String deviceRegistrationID) {
        log.info("GCM device registration complete, deviceRegistrationID = "+deviceRegistrationID);
        DeviceRegistrar.register(context, deviceRegistrationID);
        callOnComplete();
    }

    /**
     * Called when the device has been unregistered.
     * 
     * @param context
     *            the Context
     */
    @Override
    public void onUnregistered(Context context, String deviceRegistrationID) {
        log.info("Unregistered from GCM, deviceRegistrationID = "+deviceRegistrationID);
        DeviceRegistrar.unregister(context);
        callOnComplete();
    }

    /**
     * Called where there's a non-recoverable error.
     */
    @Override
    public void onError(Context context, String errorId) {
        log.error("An error has occured! Error={}", errorId);
        DeviceRegistrar.register(context, "");
        callOnComplete();
    }

    /**
     * Called when there's a \i recoverable error.
     */
    @Override
    public boolean onRecoverableError(Context context, String errorId) {
        log.error("A recoverable error has occured, trying again. Error={}", errorId);
        return true;
    }

    /**
     * Called when a cloud message has been received.
     */
    @Override
    public void onMessage(Context context, Intent intent) {
        // since this can be called when the application is not running, make sure we're
        // set to go still.
        Util.loadProperties(this);

        log.info("GCM message received.");
        MessageDisplay.displayMessage(context, intent);
    }
}
