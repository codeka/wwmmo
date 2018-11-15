package au.com.codeka.warworlds.client;

import android.util.Base64;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;

import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Notification;

/**
 * Implementation of {@link FirebaseMessagingService}.
 */
public class MessagingService extends FirebaseMessagingService {
  private static final Log log = new Log("MessagingService");

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    // TODO(developer): Handle FCM messages here.
    // If the application is in the foreground handle both data and notification messages here.
    // Also if you intend on generating your own notifications as a result of a received FCM
    // message, here is where that should be initiated. See sendNotification method below.
    log.info("From: " + remoteMessage.getFrom());
    String notificationBase64 = remoteMessage.getData().get("notification");
    if (notificationBase64 == null) {
      log.info("Notification is null, ignoring.");
      return;
    }

    Notification notification;
    try {
      notification = Notification.ADAPTER.decode(Base64.decode(notificationBase64, 0));
    } catch (IOException e) {
      log.warning("Error decoding notification, dropping.", e);
      return;
    }

    if (notification.debug_message != null) {
      App.i.getTaskRunner().runTask(() ->
          Toast.makeText(App.i, notification.debug_message, Toast.LENGTH_LONG).show(), Threads.UI);
    }

    // TODO: handle other kinds of toasts.
  }

  @Override
  public void onNewToken(String token) {
    log.info("Firebase new token: %s", token);
    // TODO: update the server.
  }
}
