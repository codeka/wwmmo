package au.com.codeka.warworlds.client.concurrency;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A queue of tasks that we can run at any time. Useful for posting runnables to threads.
 */
public class RunnableQueue {
  private final Queue<Runnable> runnables;

  public RunnableQueue(int maxQueuedItems) {
    runnables = new LinkedBlockingQueue<>(maxQueuedItems);
  }

  public void post(Runnable runnable) {
    synchronized (runnables) {
      runnables.add(runnable);
    }
  }

  /** Runs all runnables on the queue. */
  public void runAllTasks() {
    // TODO: should we pull these off into another list so the we can unblock the thread?
    synchronized (runnables) {
      while (!runnables.isEmpty()) {
        Runnable runnable = runnables.remove();
        runnable.run();
      }
    }
  }
}
