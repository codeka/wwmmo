package au.com.codeka.warworlds.client.concurrency;

/**
 * This is a class for running tasks on various threads. You can run a task on any thread defined
 * in {@link Threads}.
 */
public class TaskRunner {
  private ThreadPool backgroundThreadPool;

  public TaskRunner() {
    backgroundThreadPool = new ThreadPool(
        Threads.BACKGROUND,
        750 /* maxQueuedItems */,
        5 /* minThreads */,
        20 /* maxThreads */,
        1000 /* keepAliveMs */);
    Threads.BACKGROUND.setThreadPool(backgroundThreadPool);
  }

  public void runTask(Runnable runnable, Threads thread) {
    thread.runTask(runnable);
  }
}
