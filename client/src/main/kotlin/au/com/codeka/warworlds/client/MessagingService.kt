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
    // TODO(developer): Handle FCM messages here.
    // If the application is in the foreground handle both data and notification messages here.
    // Also if you intend on generating your own notifications as a result of a received FCM
    // message, here is where that should be initiated. See sendNotification method below.
    log.info("From: " + remoteMessage.from)
    val notificationBase64 = remoteMessage.data["notification"]
    if (notificationBase64 == null) {
      log.info("Notification is null, ignoring.")
      return
    }
    val notification: Notification
    notification = try {
      Notification.ADAPTER.decode(Base64.decode(notificationBase64, 0))
    } catch (e: IOException) {
      log.warning("Error decoding notification, dropping.", e)
      return
    }
    if (notification.debug_message != null) {
      App.taskRunner.runTask(Runnable {
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