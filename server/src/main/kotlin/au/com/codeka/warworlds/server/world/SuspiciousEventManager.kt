package au.com.codeka.warworlds.server.world

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException
import au.com.codeka.warworlds.server.concurrency.TaskRunner
import au.com.codeka.warworlds.server.concurrency.Threads
import au.com.codeka.warworlds.server.proto.SuspiciousEvent
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.Pair
import java.util.*
import javax.annotation.concurrent.GuardedBy

/**
 * A manager for managing the suspicious event store.
 */
class SuspiciousEventManager {
  /**
   * A queue of suspicious events that we're waiting to add to the store. The long in the pair is
   * for the timestamp of the event.
   */
  private val suspiciousModificationExceptionQueue: Queue<Pair<Long, SuspiciousModificationException>> = ArrayDeque()

  @GuardedBy("suspiciousModificationExceptionQueue")
  private var storeTaskQueued = false

  /**
   * Adds a suspicious event to the suspicious event store.
   *
   * @param e The [SuspiciousModificationException] representing this suspicious event.
   */
  fun addSuspiciousEvent(e: SuspiciousModificationException) {
    synchronized(suspiciousModificationExceptionQueue) {
      suspiciousModificationExceptionQueue.add(Pair(System.currentTimeMillis(), e))
      if (!storeTaskQueued) {
        TaskRunner.Companion.i.runTask(storeQueuedTask, Threads.BACKGROUND, STORE_DELAY_MS)
        storeTaskQueued = true
      }
    }
  }

  fun query( /* TODO: parameters */): Collection<SuspiciousEvent> {
    // If there's pending events to be stored, just store them now.
    synchronized(suspiciousModificationExceptionQueue) {
      if (storeTaskQueued) {
        storeQueuedTask.run()
      }
    }
    return DataStore.i.suspiciousEvents().query()
  }

  private val storeQueuedTask = Runnable {
    val exceptions = ArrayList<Pair<Long, SuspiciousModificationException>>()
    synchronized(suspiciousModificationExceptionQueue) {
      exceptions.addAll(suspiciousModificationExceptionQueue)
      suspiciousModificationExceptionQueue.clear()
      storeTaskQueued = false
    }
    val events: ArrayList<SuspiciousEvent> = ArrayList<SuspiciousEvent>()
    for (pair in exceptions) {
      val timestamp = pair.one!!
      val e = pair.two
      events.add(SuspiciousEvent.Builder()
          .timestamp(timestamp)
          .star_id(e!!.starId)
          .modification(e.modification)
          .message(e.message)
          .build())
    }
    DataStore.Companion.i.suspiciousEvents().add(events)
  }

  companion object {
    private val log = Log("SuspiciousEventManager")
    val i = SuspiciousEventManager()

    /**
     * Delay from receiving the first suspicious modification until we store it. To avoid overloading
     * the store with lots of stores.
     */
    private const val STORE_DELAY_MS = 30000
  }
}