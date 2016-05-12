package au.com.codeka.warworlds.client.concurrency;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This is a class for running tasks on various threads. You can run a task on any thread defined
 * in {@link Threads}.
 */
public class TaskRunner {
  private ThreadPool backgroundThreadPool;
  private Timer timer;

  public TaskRunner() {
    backgroundThreadPool = new ThreadPool(
        Threads.BACKGROUND,
        750 /* maxQueuedItems */,
        5 /* minThreads */,
        20 /* maxThreads */,
        1000 /* keepAliveMs */);
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
