package au.com.codeka.warworlds.server.world

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.DeviceInfo
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.Notification
import au.com.codeka.warworlds.server.store.DataStore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import java.util.*

class NotificationManager {
  fun start() {}

  /**
   * Sends the given [Notification] to the given [Empire].
   * @param empire The [Empire] to send to.
   * @param notification The [Notification] to send.
   */
  fun sendNotification(empire: Empire, notification: Notification) {
    val notificationBase64 = Base64.getEncoder().encodeToString(notification.encode())
    val devices: List<DeviceInfo> = DataStore.i.empires().getDevicesForEmpire(empire.id)
    for (device in devices) {
      sendNotification(device, notificationBase64)
    }
  }

  private fun sendNotification(device: DeviceInfo, notificationBase64: String) {
    val msg = Message.builder()
        .putData("notification", notificationBase64)
        .setToken(device.fcm_device_info!!.token)
        .build()
    try {
      val resp = FirebaseMessaging.getInstance().send(msg)
      log.info("Firebase message sent: %s", resp)
    } catch (e: FirebaseMessagingException) {
      log.error("Error sending firebase notification: %s", e.errorCode, e)
    }
  }

  companion object {
    private val log = Log("EmpireManager")
    val i = NotificationManager()
  }
}
