package au.com.codeka.warworlds.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.DeviceRegistrar;

public class NotificationMessagingService extends FirebaseMessagingService {
  private static final Log log = new Log("NotificationMessagingService");

  @Override
  public void onNewToken(String token) {
    log.debug("Refreshed token: %s", token);
    DeviceRegistrar.updateFcmToken(token);
  }
  /*
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
