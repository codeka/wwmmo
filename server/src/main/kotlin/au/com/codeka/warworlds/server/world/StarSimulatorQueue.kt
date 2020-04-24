package au.com.codeka.warworlds.server.world

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Time
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.store.StarsStore
import com.google.api.client.util.Lists
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * This class manages the star simulation queue, and schedules stars to be simulated at the
 * appropriate time.
 */
class StarSimulatorQueue private constructor() {
  private val thread: Thread
  private val stars: StarsStore = DataStore.i.stars()
  private var running = false
  private val lock = ReentrantLock()
  private val pinger = lock.newCondition()

  fun start() {
    log.info("Starting star simulation queue.")
    running = true
    thread.start()
  }

  fun stop() {
    running = false
    ping()
    try {
      thread.join()
    } catch (e: InterruptedException) {
      // Ignore.
    }
  }

  fun ping() {
    lock.withLock {
      pinger.signal()
    }
  }

  private fun run() {
    try {
      log.info("Star simulator queue starting up.")
      while (running) {
        val star = stars.nextStarForSimulate()
        var waitTime: Long
        waitTime = if (star == null) {
          log.info("No stars to simulate, sleeping for a bit.")
          10 * Time.MINUTE
        } else {
          if (star.next_simulation == null) {
            log.warning("Star #%d (%s) next_simulation is null.", star.id, star.name)
            0
          } else {
            star.next_simulation - System.currentTimeMillis()
          }
        }
        if (waitTime <= 0) {
          waitTime = 1 // 1 millisecond to ensure that we actually sleep at least a little.
        }

        // Don't sleep for more than 10 minutes, we'll just loop around and check again.
        if (waitTime > 10 * Time.MINUTE) {
          waitTime = 10 * Time.MINUTE
        }
        log.info("Star simulator sleeping for %d ms.", waitTime)
        try {
          lock.withLock {
            pinger.await(waitTime, TimeUnit.MILLISECONDS)
          }
        } catch (e: InterruptedException) {
          // Ignore.
        }
        if (star != null) {
          val startTime = System.nanoTime()
          val watchableStar = StarManager.i.getStarOrError(star.id)
          try {
            StarManager.i.modifyStar(watchableStar, Lists.newArrayList())
          } catch (e: SuspiciousModificationException) {
            // Shouldn't ever happen, as we're passing an empty list of modifications.
            log.warning("Unexpected suspicious modification.", e)
          }
          val endTime = System.nanoTime()
          log.info("Star #%d (%s) simulated in %dms",
              star.id, star.name, (endTime - startTime) / 1000000L)
        }
      }
      log.info("Star simulator queue shut down.")
    } catch (e: Exception) {
      log.error("Error in star simulation queue, star simulations are paused!", e)
    }
  }

  companion object {
    val i = StarSimulatorQueue()
    private val log = Log("StarSimulatorQueue")
  }

  init {
    thread = Thread(Runnable { this.run() }, "StarSimulateQueue")
  }
}