package au.com.codeka.warworlds.server;

import au.com.codeka.common.Log;
import au.com.codeka.common.NumberFormatter;

public class MemoryUsageLogger {
  private static final Log log = new Log("MemoryUsageLogger");

  private static Thread thread;

  public static void start() {
    if (thread != null) {
      return;
    }

    thread = new Thread(MemoryUsageLogger::threadProc);
    thread.start();
  }

  public static void stop() {
    if (thread == null) {
      return;
    }

    thread.stop();
    thread = null;
  }

  private static void threadProc() {
    while (true) {
      Runtime r = Runtime.getRuntime();

      log.info("Heap max=%s curr=%s free=%s",
          NumberFormatter.format(r.maxMemory()), NumberFormatter.format(r.totalMemory()),
          NumberFormatter.format(r.freeMemory()));

      try {
        Thread.sleep(10000);
      } catch (Exception e) {}
    }
  }
}
