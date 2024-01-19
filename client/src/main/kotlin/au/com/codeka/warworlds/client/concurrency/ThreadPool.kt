package au.com.codeka.warworlds.client.concurrency

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/** A pool of threads that we use for running things on [Threads.BACKGROUND]. */
class ThreadPool(
    private val thread: Threads,
    maxQueuedItems: Int,
    minThreads: Int,
    maxThreads: Int,
    keepAliveMs: Long) {
  val executor: ThreadPoolExecutor
  val openedThreads: HashSet<Thread> = HashSet()

  fun run(runnable: Runnable) {
    executor.execute(runnable)
  }

  fun isThread(thread: Threads): Boolean {
    return thread == this.thread
  }

  /** Returns true if the given thread is part of our thread pool. */
  fun ownsThread(thread: Thread): Boolean {
    synchronized(openedThreads) {
      return openedThreads.contains(thread)
    }
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
        // Delete any threads from openedThreads that are done.
        synchronized(openedThreads) {
          val toRemove = ArrayList<Thread>()
          for (thread in openedThreads) {
            if (!thread.isAlive) {
              toRemove.add(thread)
            }
          }
          for (thread in toRemove) {
            openedThreads.remove(thread)
          }

          val thread = Thread(r, thread.toString() + " #" + count.getAndIncrement())
          openedThreads.add(thread)
          return thread
        }
      }
    }

    val workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue(maxQueuedItems)

    executor = ThreadPoolExecutor(
        minThreads, maxThreads, keepAliveMs, TimeUnit.MILLISECONDS, workQueue, threadFactory)
  }

}