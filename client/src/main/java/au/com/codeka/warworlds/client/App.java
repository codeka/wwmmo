package au.com.codeka.warworlds.client;

import android.app.Application;

import au.com.codeka.warworlds.common.Log;

/**
 * Global {@link Application} object.
 */
public class App extends Application {
  private static final Log log = new Log("App");
  @Override
  public void onCreate() {
    super.onCreate();
    LogImpl.setup();
    log.info("App.onCreate() complete.");
  }
}
