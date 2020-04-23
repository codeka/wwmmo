package au.com.codeka.warworlds.server.concurrency

import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * A queue of tasks that we can run at any time. Useful for posting runnables to threads.
 */
class TaskQueue(maxQueuedItems: Int) {
  private val tasks: Queue<Runnable>
  fun postTask(runnable: Runnable) {
    synchronized(tasks) { tasks.add(runnable) }
  }

  /** Runs all tasks on the queue.  */
  fun runAllTasks() {
    // TODO: should we pull these off into another list so the we can unblock the thread?
    synchronized(tasks) {
      while (!tasks.isEmpty()) {
        val runnable = tasks.remove()
        runnable.run()
      }
    }
  }

  init {
    tasks = LinkedBlockingQueue(maxQueuedItems)
  }
}