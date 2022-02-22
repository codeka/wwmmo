package au.com.codeka.warworlds.client

import android.util.Base64
import android.widget.Toast
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Notification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.IOException

/**
 * Implementation of [FirebaseMessagingService].
 */
class MessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    log.info("From: " + remoteMessage.from)
    val notificationBase64 = remoteMessage.data["notification"]
    if (notificationBase64 == null) {
      log.info("Notification is null, ignoring.")
      return
    }
    val notification = try {
      Notification.ADAPTER.decode(Base64.decode(notificationBase64, 0))
    } catch (e: IOException) {
      log.warning("Error decoding notification, dropping.", e)
      return
    }
    if (notification.debug_message != null) {
      App.taskRunner.runTask({
        Toast.makeText(App, notification.debug_message, Toast.LENGTH_LONG).show()
      }, Threads.UI)
    }

    // TODO: handle other kinds of toasts.
  }

  override fun onNewToken(token: String) {
    log.info("Firebase new token: %s", token)
    // TODO: update the server.
  }

  companion object {
    private val log = Log("MessagingService")
  }
}