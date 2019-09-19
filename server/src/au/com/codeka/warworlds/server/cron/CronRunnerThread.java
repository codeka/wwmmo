package au.com.codeka.warworlds.server.cron;

import au.com.codeka.common.Log;

public class CronRunnerThread extends Thread {
  private static final Log log = new Log("CronRunnerThread");

  /** Called at startup to begin the cron thread. */
  public static void setup() {
    // TODO
  }
}
