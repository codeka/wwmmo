package au.com.codeka.warworlds.client.concurrency;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

/**
 * An enumeration of the thread types within War Worlds. Has some helper methods to ensure you run
 * on a particular thread.
 */
public enum Threads {
  /**
   * The main UI thread.
   */
  UI,

  /**
   * The OpenGL render thread. We assume there is only one of these in the whole process.
   */
  GL,

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
  @Nullable private Handler handler;
  @Nullable private RunnableQueue runnableQueue;
  @Nullable private Thread thread;
  @Nullable private ThreadPool threadPool;

  public void setThread(@NonNull Thread thread, @NonNull RunnableQueue runnableQueue) {
    checkState(!isInitialized || this.runnableQueue == runnableQueue);
    this.thread = thread;
    this.runnableQueue = runnableQueue;
    this.isInitialized = true;
  }

  public void setThread(@NonNull Thread thread, @NonNull Handler handler) {
    checkState(!isInitialized);
    this.thread = thread;
    this.handler = handler;
    this.isInitialized = true;
  }

  /** Reset this thread, unassociate it from the current thread, handler, task queue combo. */
  public void resetThread() {
    thread = null;
    handler = null;
    runnableQueue = null;
    isInitialized = false;
  }

  public void setThreadPool(@NonNull ThreadPool threadPool) {
    checkState(!isInitialized);
    this.threadPool = threadPool;
    this.isInitialized = true;
  }

  public boolean isCurrentThread() {
    checkState(isInitialized);

    if (thread != null) {
      return thread == Thread.currentThread();
    } else if (threadPool != null) {
      return threadPool.isThread(this);
    } else {
      throw new IllegalStateException("thread is null and threadPool is null");
    }
  }

  /**
   * Run the given {@link Runnable} on this thread.
   *
   * @param runnable The {@link Runnable} to run.
   */
  public void run(Runnable runnable) {
    if (handler != null) {
      handler.post(runnable);
    } else if (threadPool != null) {
      threadPool.run(runnable);
    } else if (runnableQueue != null) {
      runnableQueue.post(runnable);
    } else {
      throw new IllegalStateException("Cannot run task, no handler, taskQueue or threadPool!");
    }
  }
}
