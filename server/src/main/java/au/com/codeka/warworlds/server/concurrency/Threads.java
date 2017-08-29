package au.com.codeka.warworlds.server.concurrency;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An enumeration of the thread types within War Worlds. Has some helper methods to ensure you run
 * on a particular thread.
 */
public enum Threads {
  /**
   * A special "class" of thread that actually represents a pool of background workers.
   */
  BACKGROUND;

  public static void checkOnThread(Threads thread) {
    // Note: We don't use Preconditions.checkState because we want a nice error message and don't
    // want to allocate the string for the message every time.
    if (!thread.isCurrentThread()) {
      throw new IllegalStateException("Unexpectedly not on " + thread);
    }
  }

  public static void checkNotOnThread(Threads thread) {
    if(thread.isCurrentThread()) {
      throw new IllegalStateException("Unexpectedly on " + thread);
    }
  }

  private boolean isInitialized;
  @Nullable private TaskQueue taskQueue;
  @Nullable private Thread thread;
  @Nullable private ThreadPool threadPool;

  public void setThread(@Nonnull Thread thread, @Nonnull TaskQueue taskQueue) {
    Preconditions.checkState(!isInitialized || this.taskQueue == taskQueue);
    this.thread = thread;
    this.taskQueue = taskQueue;
    this.isInitialized = true;
  }

  public void setThreadPool(@Nonnull ThreadPool threadPool) {
    Preconditions.checkState(!isInitialized);
    this.threadPool = threadPool;
    this.isInitialized = true;
  }

  public boolean isCurrentThread() {
    Preconditions.checkState(isInitialized);

    if (thread != null) {
      return thread == Thread.currentThread();
    } else if (threadPool != null) {
      return threadPool.isThread(this);
    } else {
      throw new IllegalStateException("thread is null and threadPool is null");
    }
  }

  public void runTask(Runnable runnable) {
    if (threadPool != null) {
      threadPool.runTask(runnable);
    } else if (taskQueue != null) {
      taskQueue.postTask(runnable);
    } else {
      throw new IllegalStateException("Cannot run task, no handler, taskQueue or threadPool!");
    }
  }
}
