package au.com.codeka.warworlds.server.concurrency

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * A pool of threads that we use for running things on [Threads.BACKGROUND].
 */
class ThreadPool(
    private val thread: Threads,
    maxQueuedItems: Int,
    minThreads: Int,
    maxThreads: Int,
    keepAliveMs: Long) {
  private val executor: Executor

  fun runTask(runnable: Runnable) {
    executor.execute(runnable)
  }

  fun isThread(thread: Threads): Boolean {
    return thread == this.thread
  }

  /**
   * Constructs a new [ThreadPool].
   * @param thread The [Threads] of the thread pool, that we use to name individual threads.
   * @param maxQueuedItems The maximum number of items we'll allow to be queued.
   * @param minThreads The minimum number of threads in the thread pool.
   * @param maxThreads The maximum number of threads in the thread pool.
   * @param keepAliveMs The number of milliseconds to keep an idle thread in the thread pool.
   */
  init {
    val threadFactory: ThreadFactory = object : ThreadFactory {
      private val count = AtomicInteger(1)
      override fun newThread(r: Runnable): Thread {
        return Thread(r, thread.toString() + " #" + count.getAndIncrement())
      }
    }
    val workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue(maxQueuedItems)
    executor = ThreadPoolExecutor(
        minThreads, maxThreads, keepAliveMs, TimeUnit.MILLISECONDS, workQueue, threadFactory)
  }
}