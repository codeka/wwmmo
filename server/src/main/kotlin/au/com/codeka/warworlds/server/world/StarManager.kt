package au.com.codeka.warworlds.server.world

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.NormalRandom
import au.com.codeka.warworlds.common.Time
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.proto.Design.DesignType
import au.com.codeka.warworlds.common.sim.*
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.store.SectorsStore.SectorState
import au.com.codeka.warworlds.server.store.StarsStore
import com.google.common.collect.Lists
import java.util.*

/**
 * Manages stars and keeps the up-to-date in the data store.
 */
class StarManager private constructor() {
  private val store: StarsStore = DataStore.i.stars()
  private val stars = HashMap<Long, WatchableObject<Star>>()
  private val starModifier: StarModifier =
      StarModifier { DataStore.i.seq().nextIdentifier() }

  fun getStar(id: Long): WatchableObject<Star>? {
    var watchableStar: WatchableObject<Star>?
    synchronized(stars) {
      watchableStar = stars[id]
      if (watchableStar == null) {
        val star = store[id] ?: return null
        watchableStar = WatchableObject(star)
        watchableStar!!.addWatcher(starWatcher)
        stars[star.id] = watchableStar!!
      }
    }
    return watchableStar
  }

  fun deleteStar(id: Long) {
    val watchableStar = stars[id]
    val star: Star?
    if (watchableStar != null) {
      star = watchableStar.get()
    } else {
      star = store[id]
      if (star == null) {
        // If the star's not in the store, it doesn't exist.
        return
      }
    }
    val coord = SectorCoord.Builder().x(star.sector_x).y(star.sector_y).build()
    store.delete(id)
    synchronized(stars) { stars.remove(id) }
    SectorManager.Companion.i.forgetSector(coord)
  }

  /**
   * Add native colonies to the star with the given ID. We assume it's already eligible for one.
   */
  fun addNativeColonies(id: Long) {
    val star = getStar(id)
    synchronized(star!!.lock) {
      log.debug("Adding native colonies to star %d \"%s\"...", star.get().id, star.get().name)

      // OK, so basically any planet with a population congeniality > 500 will get a colony.
      val starBuilder = star.get().newBuilder()
      try {
        var numColonies = 0
        for (i in starBuilder.planets.indices) {
          if (starBuilder.planets[i].population_congeniality > 500) {
            starModifier.modifyStar(starBuilder, StarModification.Builder()
                .type(StarModification.MODIFICATION_TYPE.COLONIZE)
                .planet_index(i)
                .build())
            numColonies++
          }
        }

        // Create a fleet of fighters for each colony.
        val rand = NormalRandom()
        while (numColonies > 0) {
          val numShips = 100 + (rand.next() * 40).toInt()
          starModifier.modifyStar(starBuilder, StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
              .design_type(DesignType.FIGHTER)
              .count(numShips)
              .build())
          numColonies--
        }
      } catch (e: SuspiciousModificationException) {
        // Shouldn't happen, as we're creating the modifications ourselves.
        log.error("Unexpected suspicious modification.", e)
      }
      star.set(starBuilder.build())
    }
  }

  fun getStarsForEmpire(empireId: Long): ArrayList<WatchableObject<Star>?> {
    val stars = ArrayList<WatchableObject<Star>?>()
    for (id in store.getStarsForEmpire(empireId)) {
      stars.add(getStar(id!!))
    }
    return stars
  }

  @Throws(SuspiciousModificationException::class)
  fun modifyStar(
      star: WatchableObject<Star>?,
      modifications: Collection<StarModification>,
      logHandler: Simulation.LogHandler?) {
    var auxStars: MutableMap<Long?, Star>? = null
    for (modification in modifications) {
      if (modification.star_id != null) {
        auxStars = auxStars ?: TreeMap()
        if (!auxStars.containsKey(modification.star_id)) {
          val auxStar = getStar(modification.star_id)!!.get()
          auxStars[auxStar.id] = auxStar
        }
      }
    }
    modifyStar(star, auxStars?.values, modifications, logHandler)
  }

  @Throws(SuspiciousModificationException::class)
  private fun modifyStar(
      star: WatchableObject<Star>?,
      auxStars: Collection<Star>?,
      modifications: Collection<StarModification>,
      logHandler: Simulation.LogHandler?) {
    synchronized(star!!.lock) {
      val starBuilder = star.get().newBuilder()
      starModifier.modifyStar(starBuilder, auxStars, modifications, logHandler)
      completeActions(star, starBuilder, logHandler)
    }
  }

  /**
   * Call this after simulating a star to complete the actions required (e.g. if a building has
   * finished or a fleet has arrived) and also save the star to the data store.
   *
   * @param star The [<] of the star that we'll update.
   * @param starBuilder A simulated star that we need to finish up.
   * @param logHandler An optional [Simulation.LogHandler] that we'll pass log messages
   * through to. If null, we'll just do normal logging.
   * @throws SuspiciousModificationException if the
   */
  @Throws(SuspiciousModificationException::class)
  private fun completeActions(
      star: WatchableObject<Star>?,
      starBuilder: Star.Builder,
      logHandler: Simulation.LogHandler?) {
    // For any builds/moves/etc that finish in the future, make sure we schedule a job to
    // re-simulate the star then.
    var nextSimulateTime: Long? = null

    // TODO: pass this into modifyStar as well so the simulation uses the same time everywhere.
    val now = System.currentTimeMillis()

    // Any builds which have finished, we'll want to remove them and add modifications for them
    // instead.
    for (i in starBuilder.planets.indices) {
      val planet = starBuilder.planets[i]
      if (planet.colony == null || planet.colony.build_requests == null) {
        continue
      }
      val remainingBuildRequests = ArrayList<BuildRequest>()
      for (br in planet.colony.build_requests) {
        if (br.end_time <= now) {
          // It's finished. Add the actual thing it built.
          val design = DesignHelper.getDesign(br.design_type)
          if (design.design_kind == Design.DesignKind.BUILDING) {
            if (br.building_id != null) {
              // It's an existing building that we're upgrading.
              starModifier.modifyStar(starBuilder,
                  null,
                  Lists.newArrayList(StarModification.Builder()
                      .type(StarModification.MODIFICATION_TYPE.UPGRADE_BUILDING)
                      .colony_id(planet.colony.id)
                      .empire_id(planet.colony.empire_id)
                      .building_id(br.building_id)
                      .build()),
                  logHandler)
            } else {
              // It's a new building that we're creating.
              starModifier.modifyStar(
                  starBuilder,
                  null,
                  Lists.newArrayList(StarModification.Builder()
                      .type(StarModification.MODIFICATION_TYPE.CREATE_BUILDING)
                      .colony_id(planet.colony.id)
                      .empire_id(planet.colony.empire_id)
                      .design_type(br.design_type)
                      .build()),
                  logHandler)
            }
          } else {
            starModifier.modifyStar(
                starBuilder,
                null,
                Lists.newArrayList(StarModification.Builder()
                    .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
                    .empire_id(planet.colony.empire_id)
                    .design_type(br.design_type)
                    .count(br.count)
                    .build()),
                logHandler)
          }

          // Subtract the minerals it used last turn (since that won't have happening in the
          // simulation)
          val storageIndex = StarHelper.getStorageIndex(starBuilder, planet.colony.empire_id)
          var minerals = starBuilder.empire_stores[storageIndex].total_minerals
          minerals -= br.delta_minerals_per_hour * Time.HOUR / Simulation.STEP_TIME * br.progress_per_step
          if (minerals < 0) {
            minerals = 0f
          }
          starBuilder.empire_stores[storageIndex] = starBuilder.empire_stores[storageIndex].newBuilder()
              .total_minerals(minerals)
              .build()

          // TODO: add a sitrep as well
        } else {
          if (nextSimulateTime == null || nextSimulateTime > br.end_time) {
            nextSimulateTime = br.end_time
          }
          remainingBuildRequests.add(br)
        }
      }
      val planetBuilder = starBuilder.planets[i].newBuilder()
      planetBuilder.colony(planetBuilder.colony.newBuilder()
          .build_requests(remainingBuildRequests)
          .build())
      starBuilder.planets[i] = planetBuilder.build()
    }

    // Any fleets that have arrived, make sure we remove them here and add them to the destination.
    run {
      var i = 0
      while (i < starBuilder.fleets.size) {
        val fleet = starBuilder.fleets[i]
        if (fleet.state != Fleet.FLEET_STATE.MOVING || fleet.eta > now) {
          i++
          continue
        }

        // First, grab the destination star and add it there.
        val destStar = getStar(fleet.destination_star_id)
        if (destStar == null) {
          // The star doesn't exist?! Just reset it to not-moving.
          starBuilder.fleets[i] = fleet.newBuilder()
              .state(Fleet.FLEET_STATE.IDLE)
              .destination_star_id(null)
              .eta(null)
              .build()
          i++
          continue
        }
        synchronized(destStar.lock) {
          // TODO: this could deadlock, need to lock in the same order
          val destStarBuilder = destStar.get().newBuilder()
          starModifier.modifyStar(
              destStarBuilder,
              null,
              Lists.newArrayList(StarModification.Builder()
                  .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
                  .empire_id(fleet.empire_id)
                  .fleet(fleet)
                  .build()),
              logHandler)
          destStar.set(destStarBuilder.build())
        }

        // Then remove it from our star.
        starBuilder.fleets.removeAt(i)
        i--
        i++
      }
    }

    // Any fleets that have been destroyed, destroy them.
    run {
      var i = 0
      while (i < starBuilder.fleets.size) {
        val fleet = starBuilder.fleets[i]
        if (fleet.num_ships <= 0.01f) {
          starBuilder.fleets.removeAt(i)
          i--
        }
        i++
      }
    }

    // Make sure we simulate at least when the next fleet arrives
    for (i in starBuilder.fleets.indices) {
      val fleet = starBuilder.fleets[i]
      if (fleet.eta != null && (nextSimulateTime == null || nextSimulateTime > fleet.eta)) {
        if (fleet.state != Fleet.FLEET_STATE.MOVING) {
          log.warning("Fleet has non-MOVING but non-null eta, resetting to null.")
          starBuilder.fleets[i] = fleet.newBuilder().eta(null).build()
        } else {
          nextSimulateTime = fleet.eta
        }
      }
    }

    // If the star has at least one non-native colony, make sure the sector is marked non-empty
    var nonEmpty = false
    for (planet in star!!.get().planets) {
      if (planet.colony != null && planet.colony.empire_id != null) {
        nonEmpty = true
        break
      }
    }
    if (nonEmpty) {
      val coord = SectorCoord.Builder().x(star.get().sector_x).y(star.get().sector_y).build()
      val sector: WatchableObject<Sector> = SectorManager.i.getSector(coord)
      if (sector.get().state == SectorState.Empty.value) {
        DataStore.Companion.i.sectors().updateSectorState(
            coord, SectorState.Empty,
            SectorState.NonEmpty)
        sector.set(sector.get().newBuilder()
            .state(SectorState.NonEmpty.value)
            .build())
      }
    }
    starBuilder.next_simulation(nextSimulateTime)
    star.set(starBuilder.build())

    // TODO: only ping if the next simulate time is in the next 10 minutes.
    StarSimulatorQueue.i.ping()
  }

  private val starWatcher: WatchableObject.Watcher<Star> = object : WatchableObject.Watcher<Star> {
    override fun onUpdate(star: WatchableObject<Star>) {
      log.debug("Saving star %d %s", star.get().id, star.get().name)
      store.put(star.get().id, star.get())
    }
  }

  companion object {
    private val log = Log("StarManager")
    val i = StarManager()
  }
}