package au.com.codeka.warworlds.notifications;

import com.google.firebase.messaging.FirebaseMessaging;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.DeviceRegistrar;

public class NotificationManager {
  private static final Log log = new Log("NotificationManager");

  private String token;

  public void setup() {
    log.info("Getting firebase messaging token.");
    FirebaseMessaging.getInstance().getToken()
        .addOnCompleteListener(task -> {
          if (!task.isSuccessful()) {
            log.error("Fetching FCM registration token failed", task.getException());
            return;
          }

          token = task.getResult();
          log.info("Got FCM token: %s", token);
          DeviceRegistrar.updateFcmToken(token);
        });
  }
}
