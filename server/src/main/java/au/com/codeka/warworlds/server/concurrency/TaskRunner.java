package au.com.codeka.warworlds.server.concurrency;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This is a class for running tasks on various threads. You can run a task on any thread defined
 * in {@link Threads}.
 */
public class TaskRunner {
  public static TaskRunner i = new TaskRunner();

  private Timer timer;

  private TaskRunner() {
    ThreadPool backgroundThreadPool = new ThreadPool(
        Threads.BACKGROUND,
        2500 /* maxQueuedItems */,
        10 /* minThreads */,
        50 /* maxThreads */,
        5000 /* keepAliveMs */);
    Threads.BACKGROUND.setThreadPool(backgroundThreadPool);

    timer = new Timer("Timer");
  }

  public void runTask(Runnable runnable, Threads thread) {
    thread.runTask(runnable);
  }

  /** Run a task after the given delay. */
  public void runTask(final Runnable runnable, final Threads thread, int delayMs) {
    if (delayMs == 0) {
      runTask(runnable, thread);
    } else {
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          runTask(runnable, thread);
        }
      }, delayMs);
    }
  }
}
