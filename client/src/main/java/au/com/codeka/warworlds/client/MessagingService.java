package au.com.codeka.warworlds.client;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import au.com.codeka.warworlds.common.Log;

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
    log.info("Notification Message Body: " + remoteMessage.getNotification().getBody());
  }
}
