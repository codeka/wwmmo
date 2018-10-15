package au.com.codeka.warworlds.server.world;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import java.util.Base64;
import java.util.List;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.DeviceInfo;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Notification;
import au.com.codeka.warworlds.server.store.DataStore;

public class NotificationManager {
  private static final Log log = new Log("EmpireManager");
  public static final NotificationManager i = new NotificationManager();

  public void start() {
  }

  /**
   * Sends the given {@link Notification} to the given {@link Empire}.
   * @param empire The {@link Empire} to send to.
   * @param notification The {@link Notification} to send.
   */
  public void sendNotification(Empire empire, Notification notification) {
    String notificationBase64 = Base64.getEncoder().encodeToString(notification.encode());

    List<DeviceInfo> devices = DataStore.i.empires().getDevicesForEmpire(empire.id);
    for (DeviceInfo device : devices) {
      sendNotification(device, notificationBase64);
    }
  }

  private void sendNotification(DeviceInfo device, String notificationBase64) {
    Message msg = Message.builder()
        .putData("notification", notificationBase64)
        .setToken(device.fcm_device_info.token)
        .build();
    try {
      String resp = FirebaseMessaging.getInstance().send(msg);
      log.info("Firebase message sent: %s", resp);
    } catch(FirebaseMessagingException e) {
      log.error("Error sending firebase notification: %s", e.getErrorCode(), e);
    }
  }
}
