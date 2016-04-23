package au.com.codeka.warworlds.client;

import android.app.Application;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.common.Log;

/**
 * Global {@link Application} object.
 */
public class App extends Application {
  public static App i;

  private static final Log log = new Log("App");

  public App() {
    Preconditions.checkState(i == null);
    i = this;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    LogImpl.setup();
    //Util.setup(context);
    log.info("App.onCreate() complete.");
  }
}
