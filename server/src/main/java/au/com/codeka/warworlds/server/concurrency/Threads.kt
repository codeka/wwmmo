package au.com.codeka.warworlds.server.concurrency

import com.google.common.base.Preconditions

/**
 * An enumeration of the thread types within War Worlds. Has some helper methods to ensure you run
 * on a particular thread.
 */
enum class Threads {
  /**
   * A special "class" of thread that actually represents a pool of background workers.
   */
  BACKGROUND;

  private var isInitialized = false
  private var taskQueue: TaskQueue? = null
  private var thread: Thread? = null
  private var threadPool: ThreadPool? = null

  fun setThread(thread: Thread?, taskQueue: TaskQueue) {
    Preconditions.checkState(!isInitialized || this.taskQueue === taskQueue)
    this.thread = thread
    this.taskQueue = taskQueue
    this.isInitialized = true
  }

  fun setThreadPool(threadPool: ThreadPool?) {
    Preconditions.checkState(!isInitialized)
    this.threadPool = threadPool
    this.isInitialized = true
  }

  val isCurrentThread: Boolean
    get() {
      Preconditions.checkState(isInitialized)
      return if (thread != null) {
        thread === Thread.currentThread()
      } else if (threadPool != null) {
        threadPool!!.isThread(this)
      } else {
        throw IllegalStateException("thread is null and threadPool is null")
      }
    }

  fun runTask(runnable: Runnable) {
    if (threadPool != null) {
      threadPool!!.runTask(runnable)
    } else if (taskQueue != null) {
      taskQueue!!.postTask(runnable)
    } else {
      throw IllegalStateException("Cannot run task, no handler, taskQueue or threadPool!")
    }
  }

  companion object {
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