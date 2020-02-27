package au.com.codeka.warworlds.server;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import au.com.codeka.common.Log;

/**
 * This class works with the Log class to provide a concrete implementation we can use in the
 * server.
 */
public class LogImpl {
  static {
    System.setProperty("java.util.logging.manager", MyLogManager.class.getName());
  }

  private static final Logger log = Logger.getLogger("wwmmo");

  public static void setup() {
    Log.setImpl(new LogImplImpl());
  }

  private static final Level[] LevelMap = {
      Level.SEVERE,
      Level.WARNING,
      Level.INFO,
      Level.FINE
  };

  private static class LogImplImpl implements Log.LogImpl {
    @Override
    public boolean isLoggable(String tag, int level) {
      return log.isLoggable(LevelMap[level]);
    }

    @Override
    public void write(String tag, int level, String msg) {
      log.log(LevelMap[level], tag + ": " + msg);
    }
  }

  /**
   * An implementation of {@link LogManager} that doesn't reset until it's finalizer is called,
   * which happens after shutdown hooks have been run.
   */
  public static class MyLogManager extends LogManager {
    static MyLogManager instance;
    public MyLogManager() { instance = this; }

    @Override public void reset() {
      // don't reset yet.
    }

    @Override
    public void finalize() {
      super.reset();
    }
  }
}
