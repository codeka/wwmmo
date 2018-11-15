package au.com.codeka.warworlds.client.concurrency;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A pool of threads that we use for running things on {@link Threads#BACKGROUND}.
 */
public class ThreadPool {
  private final Threads thread;
  private final Executor executor;

  /**
   * Constructs a new {@link ThreadPool}.
   * @param thread The {@link Threads} of the thread pool, that we use to name individual threads.
   * @param maxQueuedItems The maximum number of items we'll allow to be queued.
   * @param minThreads The minimum number of threads in the thread pool.
   * @param maxThreads The maximum number of threads in the thread pool.
   * @param keepAliveMs The number of milliseconds to keep an idle thread in the thread pool.
   */
  public ThreadPool(
      final Threads thread,
      int maxQueuedItems,
      int minThreads,
      int maxThreads,
      int keepAliveMs) {
    this.thread = thread;

    ThreadFactory threadFactory = new ThreadFactory() {
      private final AtomicInteger count = new AtomicInteger(1);
      public Thread newThread(@NonNull Runnable r) {
        return new Thread(r, thread.toString() + " #" + count.getAndIncrement());
      }
    };

    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(maxQueuedItems);

    executor = new ThreadPoolExecutor(
        minThreads, maxThreads, keepAliveMs, TimeUnit.MILLISECONDS, workQueue, threadFactory);
  }

  public void run(Runnable runnable) {
    executor.execute(runnable);
  }

  public boolean isThread(Threads thread) {
    return thread == this.thread;
  }
}
