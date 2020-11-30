package au.com.codeka.warworlds.notifications;

import android.os.Handler;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.eventbus.EventHandler;

public class LongPoller implements Runnable {
  private static final Log log = new Log("NotificationLongPoller");

  private Thread pollThread;
  private Handler handler;
  private boolean paused;

  private long pollDelayMs;
  private static final long MAX_POLL_DELAY_MS = 30000;

  public void start() {
    log.debug("Notification long-poll starting.");
    handler = new Handler();
    restartPollThread();

    BackgroundDetector.eventBus.register(eventHandler);
  }

  private void restartPollThread() {
    pollThread = new Thread(this);
    pollThread.setDaemon(true);
    pollThread.start();
  }

  @Override
  public void run() {
    while (!paused) {
      try {
        ApiRequest apiRequest = new ApiRequest.Builder("notifications", "GET").build();
        RequestManager.i.sendRequestSync(apiRequest);
        if (apiRequest.exception() != null) {
          throw apiRequest.exception();
        }
        Messages.Notifications notificationsPb = apiRequest.body(Messages.Notifications.class);
        if (notificationsPb == null) {
          throw new Exception("Didn't get a notification, HTTP error?");
        }

        log.info("Long-poll complete, got %d notifications.",
            notificationsPb.getNotificationsCount());
        pollDelayMs = 0;
        for (Messages.Notification pb : notificationsPb.getNotificationsList()) {
          final String name = pb.getName();
          final String value = pb.getValue();
          log.info("[%s] = %s", name, value);
          handler.post(() -> Notifications.handleNotification(App.i, name, value));
        }
      } catch (Throwable e) {
        if (pollDelayMs == 0) {
          pollDelayMs = 100;
        } else {
          pollDelayMs *= 2;
        }
        if (pollDelayMs > MAX_POLL_DELAY_MS) {
          pollDelayMs = MAX_POLL_DELAY_MS;
        }

        try {
          Thread.sleep(pollDelayMs);
        } catch (InterruptedException e1) {
          // Ignored, just try again.
        }
      }
    }

    pollThread = null;
  }

  private final Object eventHandler = new Object() {
    @EventHandler(thread=EventHandler.ANY_THREAD)
    public void onBackgroundChangeEvent(BackgroundDetector.BackgroundChangeEvent event) {
      paused = event.isInBackground;
      if (!paused && pollThread == null) {
        restartPollThread();
      }
    }
  };
}
