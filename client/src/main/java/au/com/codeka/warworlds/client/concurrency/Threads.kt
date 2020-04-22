package au.com.codeka.warworlds.client.concurrency

import android.os.Handler
import com.google.common.base.Preconditions

/**
 * An enumeration of the thread types within War Worlds. Has some helper methods to ensure you run
 * on a particular thread.
 */
enum class Threads {
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

  private var isInitialized = false
  private var handler: Handler? = null
  private var runnableQueue: RunnableQueue? = null
  private var thread: Thread? = null
  private var threadPool: ThreadPool? = null

  fun setThread(thread: Thread, runnableQueue: RunnableQueue) {
    Preconditions.checkState(!isInitialized || this.runnableQueue === runnableQueue)
    this.thread = thread
    this.runnableQueue = runnableQueue
    this.isInitialized = true
  }

  fun setThread(thread: Thread, handler: Handler) {
    Preconditions.checkState(!isInitialized)
    this.thread = thread
    this.handler = handler
    this.isInitialized = true
  }

  /** Reset this thread, unassociate it from the current thread, handler, task queue combo.  */
  fun resetThread() {
    thread = null
    handler = null
    runnableQueue = null
    isInitialized = false
  }

  fun setThreadPool(threadPool: ThreadPool) {
    Preconditions.checkState(!isInitialized)
    this.threadPool = threadPool
    this.isInitialized = true
  }

  val isCurrentThread: Boolean
    get() {
      Preconditions.checkState(isInitialized)
      return when {
        thread != null -> {
          thread === Thread.currentThread()
        }
        threadPool != null -> {
          threadPool!!.isThread(this)
        }
        else -> {
          throw IllegalStateException("thread is null and threadPool is null")
        }
      }
    }

  /**
   * Run the given [Runnable] on this thread.
   *
   * @param runnable The [Runnable] to run.
   */
  fun run(runnable: Runnable) {
    when {
      handler != null -> {
        handler!!.post(runnable)
      }
      threadPool != null -> {
        threadPool!!.run(runnable)
      }
      runnableQueue != null -> {
        runnableQueue!!.post(runnable)
      }
      else -> {
        throw IllegalStateException("Cannot run task, no handler, taskQueue or threadPool!")
      }
    }
  }

  companion object {
    @JvmStatic
    fun checkOnThread(thread: Threads) {
      // Note: We don't use Preconditions.checkState because we want a nice error message and don't
      // want to allocate the string for the message every time.
      check(thread.isCurrentThread) { "Unexpectedly not on $thread" }
    }

    fun checkNotOnThread(thread: Threads) {
      check(!thread.isCurrentThread) { "Unexpectedly on $thread" }
    }
  }
}