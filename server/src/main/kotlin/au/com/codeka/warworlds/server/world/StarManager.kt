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
import com.google.common.collect.Maps
import java.util.*

/** Manages stars and keeps the up-to-date in the data store. */
class StarManager private constructor() {
  class StarNotFoundException(id: Long): Exception("Star not found: $id")

  companion object {
    private val log = Log("StarManager")
    val i = StarManager()
  }

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

  /**
   * Gets the star with the given ID
   *
   * @throws StarNotFoundException if the star does not exist.
   */
  fun getStarOrError(id: Long): WatchableObject<Star> {
    return getStar(id) ?: throw StarNotFoundException(id)
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
    val coord = SectorCoord(x = star.sector_x, y = star.sector_y)
    store.delete(id)
    synchronized(stars) { stars.remove(id) }
    SectorManager.i.forgetSector(coord)
  }

  /**
   * Add native colonies to the star with the given ID. We assume it's already eligible for one.
   */
  fun addNativeColonies(id: Long) {
    val star = getStarOrError(id)
    synchronized(star.lock) {
      log.debug("Adding native colonies to star %d \"%s\"...", star.get().id, star.get().name)

      // OK, so basically any planet with a population congeniality > 500 will get a colony.
      val mutableStar = MutableStar.from(star.get())
      try {
        var numColonies = 0
        for (planet in mutableStar.planets) {
          if (planet.populationCongeniality > 500) {
            starModifier.modifyStar(mutableStar, StarModification(
                type = StarModification.MODIFICATION_TYPE.COLONIZE,
                planet_index = planet.index))
            numColonies++
          }
        }

        // Create a fleet of fighters for each colony.
        val rand = NormalRandom()
        while (numColonies > 0) {
          val numShips = 100 + (rand.next() * 40).toInt()
          starModifier.modifyStar(mutableStar, StarModification(
              type = StarModification.MODIFICATION_TYPE.CREATE_FLEET,
              design_type = DesignType.FIGHTER,
              count = numShips))
          numColonies--
        }
      } catch (e: SuspiciousModificationException) {
        // Shouldn't happen, as we're creating the modifications ourselves.
        log.error("Unexpected suspicious modification.", e)
      }
      star.set(mutableStar.build())
    }
  }

  fun getStarsForEmpire(empireId: Long): ArrayList<WatchableObject<Star>> {
    val stars = ArrayList<WatchableObject<Star>>()
    for (id in store.getStarsForEmpire(empireId)) {
      stars.add(getStarOrError(id))
    }
    return stars
  }

  fun modifyStar(
      star: WatchableObject<Star>,
      modifications: Collection<StarModification>,
      logHandler: Simulation.LogHandler = StarModifier.EMPTY_LOG_HANDLER) {
    var auxStars: MutableMap<Long, Star>? = null
    for (modification in modifications) {
      if (modification.star_id != null) {
        auxStars = auxStars ?: TreeMap()
        if (!auxStars.containsKey(modification.star_id)) {
          val auxStar = getStarOrError(modification.star_id!!).get()
          auxStars[auxStar.id] = auxStar
        }
      }
    }
    modifyStar(star, auxStars?.values, modifications, logHandler)
  }

  private fun modifyStar(
      star: WatchableObject<Star>,
      auxStars: Collection<Star>?,
      modifications: Collection<StarModification>,
      logHandler: Simulation.LogHandler) {
    synchronized(star.lock) {
      val mutableStar = MutableStar.from(star.get())
      starModifier.modifyStar(mutableStar, modifications, auxStars, logHandler = logHandler)
      completeActions(star, mutableStar, logHandler)
      star.set(mutableStar.build())
    }
  }

  /**
   * Call this after simulating a star to complete the actions required (e.g. if a building has
   * finished or a fleet has arrived) and also save the star to the data store.
   *
   * @param star The [<] of the star that we'll update.
   * @param mutableStar A simulated star that we need to finish up.
   * @param logHandler An optional [Simulation.LogHandler] that we'll pass log messages
   * through to. If null, we'll just do normal logging.
   * @throws SuspiciousModificationException if the
   */
  private fun completeActions(
      star: WatchableObject<Star>,
      mutableStar: MutableStar,
      logHandler: Simulation.LogHandler) {
    // For any builds/moves/etc that finish in the future, make sure we schedule a job to
    // re-simulate the star then.
    var nextSimulateTime: Long? = null

    // TODO: pass this into modifyStar as well so the simulation uses the same time everywhere.
    val now = System.currentTimeMillis()

    // Any builds which have finished, we'll want to remove them and add modifications for them
    // instead.
    for (planet in mutableStar.planets) {
      val colony = planet.colony ?: continue
      val remainingBuildRequests = ArrayList<MutableBuildRequest>()
      for (br in colony.buildRequests) {
        if (br.endTime <= now) {
          // It's finished. Add the actual thing it built.
          val design = DesignHelper.getDesign(br.designType)

          // Generate a sit report for the build-complete event.
          val sitReport = SituationReport(
              empire_id = colony.id,
              planet_index = planet.index,
              star_id = mutableStar.id,
              report_time = System.currentTimeMillis(),
              build_complete_record = SituationReport.BuildCompleteRecord(
                  count = br.count,
                  design_type = br.designType,
                  upgrade = br.buildingId != null))
          val sitReports: MutableMap<Long, SituationReport> = Maps.newHashMap()
          sitReports[colony.empireId!!] = sitReport

          if (design.design_kind == Design.DesignKind.BUILDING) {
            if (br.buildingId != null) {
              // It's an existing building that we're upgrading.
              starModifier.modifyStar(mutableStar,
                  StarModification(
                      type = StarModification.MODIFICATION_TYPE.UPGRADE_BUILDING,
                      colony_id = colony.id,
                      empire_id = colony.empireId,
                      building_id = br.buildingId),
                  sitReports = sitReports,
                  logHandler = logHandler)
            } else {
              // It's a new building that we're creating.
              starModifier.modifyStar(
                  mutableStar,
                  StarModification(
                      type = StarModification.MODIFICATION_TYPE.CREATE_BUILDING,
                      colony_id = colony.id,
                      empire_id = colony.empireId,
                      design_type = br.designType),
                  sitReports = sitReports,
                  logHandler = logHandler)
            }
          } else {
            starModifier.modifyStar(
                mutableStar,
                StarModification(
                    type = StarModification.MODIFICATION_TYPE.CREATE_FLEET,
                    empire_id = colony.empireId,
                    design_type = br.designType,
                    count = br.count),
                sitReports = sitReports,
                logHandler = logHandler)
          }

          // Subtract the minerals it used last turn (since that won't have happening in the
          // simulation)
          val storageIndex = StarHelper.getStorageIndex(mutableStar, colony.empireId)
          val empireStore = mutableStar.empireStores[storageIndex]
          empireStore.totalMinerals -=
              br.mineralsDeltaPerHour * Time.HOUR / Simulation.STEP_TIME * br.progressPerStep
          if (empireStore.totalMinerals < 0) {
            empireStore.totalMinerals = 0f
          }

          // Save the situation reports to the data store.
          DataStore.i.sitReports().save(sitReports.values)
        } else {
          if (nextSimulateTime == null || nextSimulateTime > br.endTime) {
            nextSimulateTime = br.endTime
          }
          remainingBuildRequests.add(br)
        }
      }
      colony.buildRequests = remainingBuildRequests
    }

    // Any fleets that have arrived, make sure we remove them here and add them to the destination.
    run {
      var i = 0
      while (i < mutableStar.fleets.size) {
        val fleet = mutableStar.fleets[i]
        if (fleet.state != Fleet.FLEET_STATE.MOVING || fleet.eta!! > now) {
          i++
          continue
        }

        // First, grab the destination star and add it there.
        val destStar = getStar(fleet.destinationStarId!!)
        if (destStar == null) {
          // The star doesn't exist?! Just reset it to not-moving.
          fleet.state = Fleet.FLEET_STATE.IDLE
          fleet.destinationStarId = null
          fleet.eta = null
          i++
          continue
        }

        // Generate a sit report for the move-complete event.
        val sitReport = SituationReport(
            empire_id = fleet.empireId ?: 0,
            star_id = destStar.get().id,
            report_time = System.currentTimeMillis(),
            move_complete_record = SituationReport.MoveCompleteRecord(
                design_type = fleet.designType,
                fleet_id = fleet.id,
                fuel_amount = fleet.fuelAmount,
                num_ships = fleet.numShips,
                was_destroyed = false))
        val sitReports: MutableMap<Long, SituationReport> = Maps.newHashMap()
        sitReports[fleet.empireId ?: 0] = sitReport

        synchronized(destStar.lock) {
          // TODO: this could deadlock, need to lock in the same order
          val mutableDestStar = MutableStar.from(destStar.get())
          starModifier.modifyStar(
              mutableDestStar,
              StarModification(
                  type = StarModification.MODIFICATION_TYPE.CREATE_FLEET,
                  empire_id = fleet.empireId,
                  fleet = fleet.build()),
              sitReports = sitReports,
              logHandler = logHandler)
          destStar.set(mutableDestStar.build())
        }

        // Save the situation reports to the data store.
        DataStore.i.sitReports().save(sitReports.values)

        // Then remove it from our star.
        mutableStar.fleets.removeAt(i)
        i--
        i++
      }
    }

    // Any fleets that have been destroyed, destroy them.
    run {
      var i = 0
      while (i < mutableStar.fleets.size) {
        val fleet = mutableStar.fleets[i]
        if (fleet.numShips <= 0.01f) {
          mutableStar.fleets.removeAt(i)
          i--
        }
        i++
      }
    }

    // Make sure we simulate at least when the next fleet arrives
    for (i in mutableStar.fleets.indices) {
      val fleet = mutableStar.fleets[i]
      if (fleet.eta != null && (nextSimulateTime == null || nextSimulateTime > fleet.eta!!)) {
        if (fleet.state != Fleet.FLEET_STATE.MOVING) {
          log.warning("Fleet has non-MOVING but non-null eta, resetting to null.")
          fleet.eta = null
        } else {
          nextSimulateTime = fleet.eta
        }
      }
    }

    // If the star has at least one non-native colony, make sure the sector is marked non-empty
    var nonEmpty = false
    for (planet in star.get().planets) {
      val colony = planet.colony
      if (colony?.empire_id != null) {
        nonEmpty = true
        break
      }
    }
    if (nonEmpty) {
      val coord = SectorCoord(x = star.get().sector_x, y = star.get().sector_y)
      val sector: WatchableObject<Sector> = SectorManager.i.getSector(coord)
      if (sector.get().state == SectorState.Empty.value) {
        DataStore.i.sectors().updateSectorState(coord, SectorState.Empty, SectorState.NonEmpty)
        sector.set(sector.get().copy(state = SectorState.NonEmpty.value))
      }
    }
    mutableStar.nextSimulation = nextSimulateTime
    star.set(mutableStar.build())

    // TODO: only ping if the next simulate time is in the next 10 minutes.
    StarSimulatorQueue.i.ping()
  }

  private val starWatcher: WatchableObject.Watcher<Star> = object : WatchableObject.Watcher<Star> {
    override fun onUpdate(obj: WatchableObject<Star>) {
      log.debug("Saving star %d %s", obj.get().id, obj.get().name)
      store.put(obj.get().id, obj.get())
    }
  }
}
