package au.com.codeka.warworlds.server.world

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Design.DesignType
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.SectorCoord
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.proto.StarModification
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException
import au.com.codeka.warworlds.server.proto.PatreonInfo
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.store.SectorsStore.SectorState
import au.com.codeka.warworlds.server.world.generator.NewStarFinder
import com.google.common.collect.Lists
import com.patreon.PatreonAPI
import java.io.IOException
import java.util.*

/**
 * Manages empires, keeps them loaded and ensure they get saved to the data store at the right time.
 */
class EmpireManager private constructor() {
  private val watchedEmpires: MutableMap<Long, WatchableObject<Empire>>
  fun getEmpire(id: Long): WatchableObject<Empire>? {
    synchronized(watchedEmpires) {
      var watchableEmpire = watchedEmpires[id]
      if (watchableEmpire == null) {
        val empire: Empire = DataStore.i.empires().get(id) ?: return null
        watchableEmpire = watchEmpire(empire)
      }
      return watchableEmpire
    }
  }

  /**
   * Searches the database for empires matching the given query string.
   */
  fun search(query: String?): List<WatchableObject<Empire>?> {
    val empireIds: List<Long> = DataStore.i.empires().search(query)
    val empires: MutableList<WatchableObject<Empire>?> = ArrayList()
    for (id in empireIds) {
      empires.add(getEmpire(id))
    }
    return empires
  }

  /**
   * Create a new [Empire], and return it as a [WatchableObject].
   * @param name The name to give this new empire. We assume you've already confirmed that the name
   * is unique.
   *
   * @return The new empire, or null if there was an error creating the empire.
   */
  fun createEmpire(name: String?): WatchableObject<Empire>? {
    log.info("Creating new empire %s", name)
    val newStarFinder = NewStarFinder()
    return if (!newStarFinder.findStarForNewEmpire()) {
      null
    } else createEmpire(name, newStarFinder)
  }

  /**
   * Create a new [Empire], and return it as a [WatchableObject].
   * @param name The name to give this new empire. We assume you've already confirmed that the name
   * is unique.
   * @param newStarFinder A [NewStarFinder] that's already found the star we will put the
   * empire on.
   *
   * @return The new empire, or null if there was an error creating the empire.
   */
  fun createEmpire(name: String?, newStarFinder: NewStarFinder): WatchableObject<Empire>? {
    val id: Long = DataStore.i.seq().nextIdentifier()
    val star: WatchableObject<Star>? = StarManager.i.getStar(newStarFinder.star!!.id)
    if (star == null) {
      // Shouldn't happen, NewStarFinder should actually find a star.
      log.error("Unknown star?")
      return null
    }
    try {
      StarManager.Companion.i.modifyStar(star, Lists.newArrayList(
          StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.EMPTY_NATIVE)
              .build(),
          createFleet(id, DesignType.COLONY_SHIP, 11),
          createFleet(id, DesignType.FIGHTER, 100),
          createFleet(id, DesignType.FIGHTER, 100),
          createFleet(id, DesignType.FIGHTER, 100),
          createFleet(id, DesignType.FIGHTER, 150),
          createFleet(id, DesignType.FIGHTER, 150),
          createFleet(id, DesignType.FIGHTER, 200),
          createFleet(id, DesignType.FIGHTER, 200),
          createFleet(id, DesignType.TROOP_CARRIER, 150),
          createFleet(id, DesignType.TROOP_CARRIER, 150),
          createFleet(id, DesignType.TROOP_CARRIER, 150),
          createFleet(id, DesignType.TROOP_CARRIER, 200),
          createFleet(id, DesignType.TROOP_CARRIER, 200),
          createFleet(id, DesignType.TROOP_CARRIER, 250),
          createFleet(id, DesignType.TROOP_CARRIER, 250),
          createFleet(id, DesignType.SCOUT, 20),
          createFleet(id, DesignType.SCOUT, 0),
          StarModification.Builder()
              .empire_id(id)
              .type(StarModification.MODIFICATION_TYPE.COLONIZE)
              .planet_index(newStarFinder.planetIndex)
              .build()
      ), null /* logHandler */)
    } catch (e: SuspiciousModificationException) {
      // Shouldn't happen, none of these should be suspicious...
      log.error("Unexpected suspicious modification.", e)
      return null
    }
    val empire = Empire.Builder()
        .display_name(name)
        .id(id)
        .home_star(newStarFinder.star)
        .build()
    DataStore.Companion.i.empires().put(id, empire)
    DataStore.Companion.i.sectors().updateSectorState(
        SectorCoord.Builder().x(star.get().sector_x).y(star.get().sector_y).build(),
        SectorState.Empty,
        SectorState.NonEmpty)
    return watchEmpire(empire)
  }

  /**
   * Refreshes the Patreon data for the given [Empire], by making a request to Patreon's
   * server.
   */
  @Throws(IOException::class)
  fun refreshPatreonInfo(
      empire: WatchableObject<Empire>?, patreonInfo: PatreonInfo) {
    var patreonInfo: PatreonInfo = patreonInfo
    log.info("Refreshing Patreon pledges for %d (%s).",
        empire!!.get().id, empire.get().display_name)
    val apiClient = PatreonAPI(patreonInfo.access_token)
    val userJson = apiClient.fetchUser()
    val user = userJson.get()
    var maxPledge = 0
    if (user.pledges != null) {
      for (pledge in user.pledges) {
        if (pledge.amountCents > maxPledge) {
          maxPledge = pledge.amountCents
        }
      }
    }
    patreonInfo = patreonInfo.newBuilder()
        .full_name(user.fullName)
        .about(user.about)
        .discord_id(user.discordId)
        .patreon_url(user.url)
        .email(user.email)
        .image_url(user.imageUrl)
        .max_pledge(maxPledge)
        .build()
    DataStore.Companion.i.empires().savePatreonInfo(empire.get()!!.id, patreonInfo)
  }

  private fun watchEmpire(empire: Empire): WatchableObject<Empire>? {
    var watchableEmpire: WatchableObject<Empire>?
    synchronized(watchedEmpires) {
      watchableEmpire = watchedEmpires[empire.id]
      if (watchableEmpire != null) {
        watchableEmpire!!.set(empire)
      } else {
        watchableEmpire = WatchableObject(empire)
        watchedEmpires.put(watchableEmpire!!.get().id, watchableEmpire!!)
      }
    }
    return watchableEmpire
  }

  companion object {
    private val log = Log("EmpireManager")
    val i = EmpireManager()
    private fun createFleet(empireId: Long, designType: DesignType, count: Int): StarModification {
      return StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
          .empire_id(empireId)
          .design_type(designType)
          .count(count)
          .full_fuel(true)
          .build()
    }
  }

  init {
    watchedEmpires = HashMap()
  }
}