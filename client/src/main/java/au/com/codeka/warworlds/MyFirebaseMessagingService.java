package au.com.codeka.warworlds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.model.EmpireManager;

import com.google.firebase.messaging.FirebaseMessagingService;

/**
 * Receive a push message from Firebase Cloud Messaging.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
  private static final Log log = new Log("MyFirebaseMessagingService");

  public static final String PROJECT_ID = "990931198580";
/*
  public MyFirebaseMessagingService() {
    super(PROJECT_ID);
  }

  public static void register(Activity activity) {
    GCMRegistrar.register(activity, PROJECT_ID);
  }

  public static void unregister(Activity activity) {
    GCMRegistrar.unregister(activity);
  }

  @Override
  public void onRegistered(Context context, String gcmRegistrationID) {
    log.info("GCM device registration complete, gcmRegistrationID = %s", gcmRegistrationID);
    DeviceRegistrar.updateGcmRegistration(gcmRegistrationID);
  }

  @Override
  public void onUnregistered(Context context, String deviceRegistrationID) {
    log.info("Unregistered from GCM, deviceRegistrationID = " + deviceRegistrationID);
    DeviceRegistrar.unregister(false);
  }

  @Override
  public void onError(Context context, String errorId) {
    log.error("An error has occurred! Error: %s", errorId);
  }

  @Override
  public boolean onRecoverableError(Context context, String errorId) {
    log.error("A recoverable error has occurred, trying again. Error: %s", errorId);
    return true;
  }

  @Override
  public void onMessage(Context context, Intent intent) {
    // since this can be called when the application is not running, make sure we're
    // set to go still.
    Util.loadProperties();
    Util.setup(context);

    Bundle extras = intent.getExtras();
    if (extras != null) {
      if (extras.containsKey("sitrep")) {
        Notifications.handleNotification(context, "sitrep", extras.getString("sitrep"));
      }
      if (extras.containsKey("chat")) {
        Notifications.handleNotification(context, "chat", extras.getString("chat"));
      }
      if (extras.containsKey("empire_updated")) {
        EmpireManager.i.refreshEmpire();
      }
    }
  }
  */
}
