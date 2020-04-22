package au.com.codeka.warworlds.client.concurrency

import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/** A queue of tasks that we can run at any time. Useful for posting runnables to threads. */
class RunnableQueue(maxQueuedItems: Int) {
  private val runnables: Queue<Runnable>
  fun post(runnable: Runnable) {
    synchronized(runnables) { runnables.add(runnable) }
  }

  /** Runs all runnables on the queue.  */
  fun runAllTasks() {
    // TODO: should we pull these off into another list so the we can unblock the thread?
    synchronized(runnables) {
      while (!runnables.isEmpty()) {
        val runnable = runnables.remove()
        runnable.run()
      }
    }
  }

  init {
    runnables = LinkedBlockingQueue(maxQueuedItems)
  }
}