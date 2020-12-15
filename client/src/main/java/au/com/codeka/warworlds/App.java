package au.com.codeka.warworlds;

import android.app.Application;

import com.google.firebase.FirebaseApp;

import au.com.codeka.warworlds.notifications.NotificationManager;

public class App extends Application {
  public static App i;

  private final NotificationManager notificationManager;

  public App() {
    i = this;

    notificationManager = new NotificationManager();
  }

  public NotificationManager getNotificationManager() {
    return notificationManager;
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

    FirebaseApp.initializeApp(this);
  }
}

