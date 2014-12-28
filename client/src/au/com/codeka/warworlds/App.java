package au.com.codeka.warworlds;

import android.support.multidex.MultiDexApplication;

public class App extends MultiDexApplication {
  public static App i;

  public App() {
        i = this;
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
  }
}

