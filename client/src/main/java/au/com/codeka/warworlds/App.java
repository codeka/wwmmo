package au.com.codeka.warworlds;

import android.app.Application;
import android.os.Handler;

import com.google.firebase.FirebaseApp;

import au.com.codeka.warworlds.concurrency.TaskRunner;
import au.com.codeka.warworlds.concurrency.Threads;
import au.com.codeka.warworlds.notifications.NotificationManager;

public class App extends Application {
  public static App i;

  private final NotificationManager notificationManager;
  private final TaskRunner taskRunner;

  public App() {
    i = this;

    notificationManager = new NotificationManager();
    taskRunner = new TaskRunner();
  }

  public NotificationManager getNotificationManager() {
    return notificationManager;
  }

  public TaskRunner getTaskRunner() {
    return taskRunner;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    // use our time zone provider, rather than the Joda-provided one, which is heaps faster.
    System.setProperty("org.joda.time.DateTimeZone.Provider",
        "au.com.codeka.common.FastDateTimeZoneProvider");

    // this will force the BackgroundRunner's handler to initialize on the main thread.
    try {
      Class.forName("au.com.codeka.BackgroundRunner");
    } catch (ClassNotFoundException e) {
    }

    Threads.UI.setThread(Thread.currentThread(), new Handler());

    FirebaseApp.initializeApp(this);
  }
}

