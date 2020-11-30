package au.com.codeka.warworlds.notifications;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.DeviceRegistrar;
import au.com.codeka.warworlds.Notifications;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.EmpireManager;

public class NotificationMessagingService extends FirebaseMessagingService {
  private static final Log log = new Log("NotificationMessagingService");

  @Override
  public void onNewToken(String token) {
    log.debug("Refreshed token: %s", token);
    DeviceRegistrar.updateFcmToken(token);
  }

  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    super.onMessageReceived(remoteMessage);
    log.info("Message: %s", remoteMessage.getFrom());

    // since this can be called when the application is not running, make sure we're
    // set to go still.
    Util.loadProperties();
    Util.setup(this);

    Map<String, String> data = remoteMessage.getData();
    if (data.containsKey("sitrep")) {
      Notifications.handleNotification(this, "sitrep", data.get("sitrep"));
    }
    if (data.containsKey("chat")) {
      Notifications.handleNotification(this, "chat", data.get("chat"));
    }
    if (data.containsKey("debug-msg")) {
      Notifications.handleNotification(this, "debug-msg", data.get("debug-msg"));
    }
    if (data.containsKey("empire_updated")) {
      EmpireManager.i.refreshEmpire();
    }
  }
}
