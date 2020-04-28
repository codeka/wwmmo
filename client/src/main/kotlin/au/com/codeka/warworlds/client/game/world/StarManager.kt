package au.com.codeka.warworlds.client.game.world

import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.store.StarCursor
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.sim.Simulation
import au.com.codeka.warworlds.common.sim.StarModifier
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException
import com.google.common.collect.Lists
import java.util.*

/**
 * Manages the [Star]s we keep cached and stuff.
 */
object StarManager {
  private val log = Log("StarManager")
  private val stars = App.dataStore.stars()
  private val starModifier = StarModifier { 0 }

  fun create() {
    App.eventBus.register(eventListener)
  }

  /** Gets the star with the given ID. Might return null if don't have that star cached yet.  */
  fun getStar(id: Long): Star? {
    // TODO: this probably shouldn't happen on a UI thread...
    return stars[id]
  }

  val myStars: StarCursor
    get() = stars.myStars

  fun searchMyStars(search: String?): StarCursor {
    return stars.searchMyStars(search!!)
  }

  /**
   * Gets the most recent value of last_simulation out of all our empire's stars. This is sent to
   * the server in the [au.com.codeka.warworlds.common.proto.HelloPacket], so that the server
   * can update us on all our stars that have been updated since we last connected.
   */
  val lastSimulationOfOurStar: Long?
    get() = stars.lastSimulationOfOurStar

  /**
   * Queue up the given [Star] to be simulated. The star will be simulated in the background
   * and will be posted to the event bus when complete.
   */
  fun queueSimulateStar(star: Star) {
    // Something more scalable that just queuing them all to the background thread pool...
    App.taskRunner.runTask(Runnable { simulateStarSync(star) }, Threads.BACKGROUND)
  }

  /**
   * Simulate the star on the current thread.
   */
  fun simulateStarSync(star: Star) {
    val starBuilder = star.newBuilder()
    Simulation().simulate(starBuilder)

    // No need to save the star, it's just a simulation, but publish it to the event bus so
    // clients can see it.
    App.eventBus.publish(starBuilder.build())
  }

  fun updateStar(star: Star, modificationBuilder: StarModification.Builder) {
    // Be sure to record our empire_id in the request.
    val modification = modificationBuilder
        .empire_id(EmpireManager.getMyEmpire().id)
        .build()
    App.taskRunner.runTask(Runnable {

      // If there's any auxiliary stars, grab them now, too.
      var auxiliaryStars: MutableList<Star>? = null
      if (modification.star_id != null) {
        auxiliaryStars = ArrayList()
        val s = stars[modification.star_id]
        if (s != null) {
          auxiliaryStars.add(s)
        }
      }

      // Modify the star.
      val starBuilder = star.newBuilder()
      try {
        starModifier.modifyStar(starBuilder, Lists.newArrayList(modification), auxiliaryStars)
      } catch (e: SuspiciousModificationException) {
        // Mostly we don't care about these on the client, but it'll be good to log them.
        log.error("Unexpected suspicious modification.", e)
        return@Runnable
      }

      // Save the now-modified star.
      val newStar = starBuilder.build()
      stars.put(star.id, newStar, EmpireManager.getMyEmpire())
      App.eventBus.publish(newStar)

      // Send the modification to the server as well.
      App.server.send(Packet.Builder()
          .modify_star(ModifyStarPacket.Builder()
              .star_id(star.id)
              .modification(Lists.newArrayList(modification))
              .build())
          .build())
    }, Threads.BACKGROUND)
  }

  private val eventListener: Any = object : Any() {
    /**
     * When the server that a star has been updated, we'll want to update our cached copy of it.
     */
    @EventHandler(thread = Threads.BACKGROUND)
    fun onStarUpdatedPacket(pkt: StarUpdatedPacket) {
      log.info("Stars updating, saving to database.")
      val startTime = System.nanoTime()
      val values: MutableMap<Long?, Star> = HashMap()
      for (star in pkt.stars) {
        App.eventBus.publish(star)
        values[star.id] = star
      }
      stars.putAll(values, EmpireManager.getMyEmpire())
      val endTime = System.nanoTime()
      log.info("Updated %d stars in DB in %d ms", pkt.stars.size, (endTime - startTime) / 1000000L)
    }
  }
}