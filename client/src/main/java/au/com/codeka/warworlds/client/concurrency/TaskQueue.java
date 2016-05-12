package au.com.codeka.warworlds.client.concurrency;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A queue of tasks that we can run at any time. Useful for posting runnables to threads.
 */
public class TaskQueue {
  private final Queue<Runnable> tasks;

  public TaskQueue(int maxQueuedItems) {
    tasks = new LinkedBlockingQueue<>(maxQueuedItems);
  }

  public void postTask(Runnable runnable) {
    synchronized (tasks) {
      tasks.add(runnable);
    }
  }

  /** Runs all tasks on the queue. */
  public void runAllTasks() {
    // TODO: should we pull these off into another list so the we can unblock the thread?
    synchronized (tasks) {
      while (!tasks.isEmpty()) {
        Runnable runnable = tasks.remove();
        runnable.run();
      }
    }
  }
}
