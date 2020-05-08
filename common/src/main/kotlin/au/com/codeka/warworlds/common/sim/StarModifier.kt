package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Time
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.sim.DesignHelper.getDesign
import au.com.codeka.warworlds.common.sim.EmpireHelper.isSameEmpire
import au.com.codeka.warworlds.common.sim.FleetHelper.hasEffect
import au.com.codeka.warworlds.common.sim.FleetHelper.isFriendly
import au.com.codeka.warworlds.common.sim.FleetHelper.isOwnedBy
import au.com.codeka.warworlds.common.sim.StarHelper.distanceBetween
import au.com.codeka.warworlds.common.sim.StarHelper.getStorageIndex
import com.google.common.base.Preconditions
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

/** Class for handling modifications to a star. */
class StarModifier(private val identifierGenerator: () -> Long) {
  /**
   * Modify a star, and possibly other auxiliary stars.
   */
  fun modifyStar(
      star: Star.Builder,
      modification: StarModification,
      auxStars: Collection<Star>? = null,
      sitReports: MutableMap<Long, SituationReport.Builder>? = null,
      logHandler: Simulation.LogHandler? = null) {
    modifyStar(star, Lists.newArrayList(modification), auxStars, sitReports, logHandler)
  }

  /**
   * Modify a star, and possibly other auxiliary stars.
   *
   * @param star The [Star.Builder] that we're modifying. The star is simulated before and
   *        after being modified.
   * @param auxStars A collection of auxiliary stars that we may need while modifying this star (for
   *        example, MOVE_FLEET needs to know about the destination). These are not simulated.
   * @param modifications The list of [StarModification]s to apply.
   * @param sitReports If specified, we'll populate the situation reports whenever something report-
   *        worthy happens (usually combat).
   * @param logHandler An optional [Simulation.LogHandler] that we'll pass through all log
   *        messages to.
   * @throws SuspiciousModificationException when the modification seems suspicious, or isn't
   *         otherwise allowed (e.g. you're trying to modify another empire's star, for example).
   */
  fun modifyStar(
      star: Star.Builder,
      modifications: Collection<StarModification>,
      auxStars: Collection<Star>? = null,
      sitReports: MutableMap<Long, SituationReport.Builder>? = null,
      logHandler: Simulation.LogHandler? = null) {
    val log = logHandler ?: EMPTY_LOG_HANDLER
    log.log("Applying " + modifications.size + " modifications.")
    if (modifications.isNotEmpty()) {
      Simulation(false).simulate(star)
      for (modification in modifications) {
        applyModification(star, auxStars, modification, log)
      }
    }
    Simulation(log).simulate(star, sitReports)
  }

  private fun applyModification(
      star: Star.Builder,
      auxStars: Collection<Star>?,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    when (modification.type) {
      StarModification.MODIFICATION_TYPE.COLONIZE -> applyColonize(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.CREATE_FLEET -> applyCreateFleet(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.CREATE_BUILDING -> applyCreateBuilding(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.ADJUST_FOCUS -> applyAdjustFocus(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST -> applyAddBuildRequest(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.DELETE_BUILD_REQUEST -> applyDeleteBuildRequest(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.SPLIT_FLEET -> applySplitFleet(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.MERGE_FLEET ->  applyMergeFleet(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.MOVE_FLEET -> applyMoveFleet(star, auxStars, modification, logHandler)
      StarModification.MODIFICATION_TYPE.EMPTY_NATIVE -> applyEmptyNative(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.UPGRADE_BUILDING -> applyUpgradeBuilding(star, modification, logHandler)
      StarModification.MODIFICATION_TYPE.ATTACK_COLONY -> applyAttackColony(star, modification, logHandler)
      else -> {
        logHandler.log("Unknown or unexpected modification type: ${modification.type}")
        log.error("Unknown or unexpected modification type: ${modification.type}")
      }
    }
  }

  private fun applyColonize(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.COLONIZE)
    logHandler!!.log(String.format(Locale.US, "- colonizing planet #%d", modification.planet_index))

    // When we destroy the colonyship, we'll take it's energy and add it to the star's supply.
    var remainingFuel: Float = 0f

    // Destroy a colony ship, unless this is a native colony.
    if (modification.empire_id != null) {
      var found = false
      for (i in star.fleets.indices) {
        val fleet = star.fleets[i]
        if (fleet.design_type == Design.DesignType.COLONY_SHIP &&
            fleet.empire_id == modification.empire_id) {
          // TODO: check for cryogenics
          if (ceil(fleet.num_ships.toDouble()) == 1.0) {
            remainingFuel = star.fleets[i].fuel_amount
            star.fleets.removeAt(i)
          } else {
            // Make sure we don't have too much fuel.
            val design = getDesign(fleet.design_type)
            val maxFuelAmount = design.fuel_size * (fleet.num_ships - 1)
            val fuelAmount = max(fleet.fuel_amount, maxFuelAmount)
            remainingFuel = fleet.fuel_amount - fuelAmount
            star.fleets[i] = fleet.newBuilder()
                .num_ships(fleet.num_ships - 1)
                .fuel_amount(fuelAmount)
                .build()
          }
          found = true
          break
        }
      }
      if (!found) {
        logHandler.log("  no colonyship, cannot colonize.")
        return
      }
    }
    star.planets[modification.planet_index] = star.planets[modification.planet_index].newBuilder()
        .colony(Colony.Builder()
            .cooldown_end_time(System.currentTimeMillis() + 15 * Time.MINUTE)
            .empire_id(modification.empire_id)
            .focus(ColonyFocus.Builder()
                .construction(0.1f)
                .energy(0.3f)
                .farming(0.3f)
                .mining(0.3f)
                .build())
            .id(identifierGenerator())
            .population(100.0f)
            .defence_bonus(1.0f)
            .build())
        .build()
    logHandler.log(String.format(Locale.US,
        "  colonized: colony_idCOLONIZE=%d",
        star.planets[modification.planet_index].colony.id))

    // if there's no storage for this empire, add one with some defaults now.
    val storageIndex = getStorageIndex(star, modification.empire_id)
    if (storageIndex < 0) {
      val storage = createDefaultStorage(modification.empire_id)
      storage.total_energy += remainingFuel
      star.empire_stores.add(storage.build())
    } else {
      star.empire_stores[storageIndex] = star.empire_stores[storageIndex].newBuilder()
          .total_energy(star.empire_stores[storageIndex].total_energy + remainingFuel)
          .build()
    }
  }

  private fun createDefaultStorage(empireId: Long?): EmpireStorage.Builder {
    return EmpireStorage.Builder()
        .empire_id(empireId)
        .total_goods(100.0f).total_minerals(25000f).total_energy(20000f)
        .max_goods(200.0f).max_minerals(5000.0f).max_energy(5000.0f)
  }

  private fun applyCreateFleet(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.CREATE_FLEET)
    var attack = false
    if (modification.fleet == null || modification.fleet.stance == Fleet.FLEET_STANCE.AGGRESSIVE) {
      for (fleet in star.fleets) {
        if (!isFriendly(fleet, modification.empire_id)) {
          attack = true
        }
      }
    }

    // First, if there's any fleets that are not friendly and aggressive, they should change to
    // attacking as well.
    var numAttacking = 0
    for (i in star.fleets.indices) {
      val fleet = star.fleets[i]
      if (!isFriendly(fleet, modification.empire_id)
          && fleet.stance == Fleet.FLEET_STANCE.AGGRESSIVE) {
        star.fleets[i] = fleet.newBuilder()
            .state(Fleet.FLEET_STATE.ATTACKING)
            .state_start_time(System.currentTimeMillis())
            .build()
        numAttacking++
      }
    }
    var fuelAmount: Float
    val design = getDesign(
        if (modification.fleet == null) modification.design_type else modification.fleet.design_type)
    val numShips = if (modification.fleet == null) modification.count.toFloat() else modification.fleet.num_ships
    if (modification.full_fuel != null && modification.full_fuel) {
      fuelAmount = design.fuel_size * numShips
    } else {
      // It'll be refueled when we simulate, if there's any energy on the star.
      fuelAmount = 0f
      if (modification.fleet != null) {
        fuelAmount = modification.fleet.fuel_amount
      }
    }

    // Now add the fleet itself.
    logHandler!!.log(String.format(Locale.US, "- creating fleet (%s) numAttacking=%d fuel=%.2f",
        if (attack) "attacking" else "not attacking",
        numAttacking, fuelAmount))
    if (modification.fleet != null) {
      star.fleets.add(modification.fleet.newBuilder()
          .id(identifierGenerator())
          .state(if (attack) Fleet.FLEET_STATE.ATTACKING else Fleet.FLEET_STATE.IDLE)
          .state_start_time(System.currentTimeMillis())
          .fuel_amount(fuelAmount)
          .destination_star_id(null)
          .eta(null)
          .build())

      // If there's an existing fleet, and it has a SCOUT_SHIP effect, then generate a scout report.
      if (hasEffect(modification.fleet, Design.EffectType.SCOUT_SHIP)) {
        generateScoutReport(star, modification, logHandler)
      }
    } else {
      star.fleets.add(Fleet.Builder() //TODO: .alliance_id()
          .design_type(modification.design_type)
          .empire_id(modification.empire_id)
          .id(identifierGenerator())
          .num_ships(modification.count.toFloat())
          .fuel_amount(fuelAmount)
          .stance(Fleet.FLEET_STANCE.AGGRESSIVE)
          .state(if (attack) Fleet.FLEET_STATE.ATTACKING else Fleet.FLEET_STATE.IDLE)
          .state_start_time(System.currentTimeMillis())
          .build())
    }
  }

  private fun generateScoutReport(
      star: Star.Builder, modification: StarModification, logHandler: Simulation.LogHandler?) {
    val scoutReport = ScoutReport.Builder()
        .report_time(System.currentTimeMillis()) // TODO: record a better time
        .star_id(star.id)
        .empire_id(modification.empire_id)
    for (fleet in star.fleets) {
      // Skip fleets that are moving, they're technically not "on" this star.
      if (fleet.state == Fleet.FLEET_STATE.MOVING) {
        continue
      }
      val design = getDesign(fleet.design_type)
      // We'll put the fuel as max so even with a scout report, you can't tell.
      scoutReport.fleets.add(fleet.newBuilder()
          .fuel_amount(design.fuel_size * fleet.num_ships)
          .build())
    }
    for (p in star.planets) {
      val planet = p.newBuilder()
      if (planet.colony != null) {
        // Rebuild the colony with only the things we want.
        planet.colony(Colony.Builder()
            .id(planet.colony.id)
            .empire_id(planet.colony.empire_id)
            .population(planet.colony.population)
            .build())
      }
      scoutReport.planets.add(planet.build())
    }

    // Now check whether there's already a report for this empire and if there is we'll just replace
    // that one with this one.
    var existingReport = false
    for (i in star.scout_reports.indices) {
      if (isSameEmpire(star.scout_reports[i].empire_id, modification.empire_id)) {
        existingReport = true
        star.scout_reports[i] = scoutReport.build()
        break
      }
    }
    if (!existingReport) {
      star.scout_reports.add(scoutReport.build())
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyCreateBuilding(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.CREATE_BUILDING)
    val planet = getPlanetWithColony(star, modification.colony_id)
    if (planet != null) {
      if (!isSameEmpire(planet.colony.empire_id, modification.empire_id)) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to create building on planet for different empire. colony.empire_id=%d",
            planet.colony.empire_id)
      }
      logHandler.log("- creating building, colony_id=${modification.colony_id}")
      val colony = planet.colony.newBuilder()
      colony.buildings.add(Building.Builder()
          .id(identifierGenerator())
          .design_type(modification.design_type)
          .level(1)
          .build())
      star.planets[planet.index] = planet.newBuilder()
          .colony(colony.build())
          .build()
    } else {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to create building on colony that does not exist. colony_id=%d",
          modification.colony_id)
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyAdjustFocus(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.ADJUST_FOCUS)
    val planet = getPlanetWithColony(star, modification.colony_id)
    if (planet != null) {
      if (!isSameEmpire(planet.colony.empire_id, modification.empire_id)) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to adjust focus on planet for different empire. colony.empire_id=%d",
            planet.colony.empire_id)
      }
      logHandler!!.log("- adjusting focus.")
      star.planets[planet.index] = planet.newBuilder()
          .colony(planet.colony.newBuilder()
              .focus(modification.focus)
              .build())
          .build()
    } else {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to adjust focus on a colony that does not exist. colony_id=%d",
          modification.colony_id)
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyAddBuildRequest(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)
    val planet = getPlanetWithColony(star, modification.colony_id)
    if (planet != null) {
      val colonyBuilder = planet.colony.newBuilder()
      if (!isSameEmpire(colonyBuilder.empire_id, modification.empire_id)) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to add build request on colony that does belong to you. colony_id=%d " +
                "empire_id=%d modification_empire_id=%d",
            modification.colony_id,
            colonyBuilder.empire_id,
            modification.empire_id)
      }

      // If the build request is for a ship and this colony doesn't have a shipyard, you can't
      // build. That's suspicious.
      val design = getDesign(modification.design_type)
      if (design.design_kind == Design.DesignKind.SHIP) {
        var hasShipyard = false
        for (building in colonyBuilder.buildings) {
          if (building.design_type == Design.DesignType.SHIPYARD) {
            hasShipyard = true
          }
        }
        if (!hasShipyard) {
          throw SuspiciousModificationException(
              star.id,
              modification,
              "Attempt to build ship with no shipyard present.")
        }
      }

      // If this is not a building, then building_id must be null.
      if (design.design_kind != Design.DesignKind.BUILDING && modification.building_id != null) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Cannot upgrade something that is not a building.")
      }

      // If the build request has a building_id, then that building should be on this colony.
      if (modification.building_id != null) {
        var found = false
        for (bldg in colonyBuilder.buildings) {
          if (bldg.id == modification.building_id) {
            found = true
          }
        }
        if (!found) {
          throw SuspiciousModificationException(
              star.id,
              modification,
              "Cannot upgrade a building that doesn't exist: #%d", modification.building_id)
        }
      }
      var count = 1
      if (design.design_kind == Design.DesignKind.SHIP) {
        count = modification.count
      }
      logHandler!!.log("- adding build request")
      colonyBuilder.build_requests.add(BuildRequest.Builder()
          .id(identifierGenerator())
          .design_type(modification.design_type)
          .building_id(modification.building_id)
          .start_time(System.currentTimeMillis())
          .count(count)
          .progress(0.0f)
          .build())
      star.planets[planet.index] = planet.newBuilder()
          .colony(colonyBuilder.build())
          .build()
    } else {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to add build request on colony that does not exist. colony_id=%d",
          modification.colony_id)
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyDeleteBuildRequest(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.DELETE_BUILD_REQUEST)
    var planet: Planet? = null
    var buildRequest: BuildRequest? = null
    for (p in star.planets) {
      if (p.colony != null) {
        for (br in p.colony.build_requests) {
          if (br.id == modification.build_request_id) {
            if (!isSameEmpire(p.colony.empire_id, modification.empire_id)) {
              throw SuspiciousModificationException(
                  star.id,
                  modification,
                  "Attempt to delete build request for different empire. building.empire_id=%d",
                  p.colony.empire_id)
            }
            planet = p
            buildRequest = br
            break
          }
        }
        if (planet != null) {
          break
        }
      }
    }
    if (planet == null) {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to delete build request that does not exist. build_request_id=%d",
          modification.build_request_id)
    }
    val idToDelete = buildRequest!!.id
    logHandler!!.log("- deleting build request")
    val colonyBuilder = planet.colony.newBuilder()
    colonyBuilder.build_requests(
        Lists.newArrayList(
            Iterables.filter(planet.colony.build_requests) { br: BuildRequest? -> br!!.id != idToDelete }))
    star.planets[planet.index] = planet.newBuilder()
        .colony(colonyBuilder.build())
        .build()
  }

  @Throws(SuspiciousModificationException::class)
  private fun applySplitFleet(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.SPLIT_FLEET)
    val fleetIndex = findFleetIndex(star, modification.fleet_id)
    if (fleetIndex >= 0) {
      var fleet = star.fleets[fleetIndex].newBuilder()
      if (!isSameEmpire(fleet.empire_id, modification.empire_id)) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to split fleet of different empire. fleet.empire_id=%d",
            fleet.empire_id)
      }
      val design = getDesign(fleet.design_type)
      val fuelFraction = fleet.fuel_amount / (design.fuel_size * fleet.num_ships)
      logHandler!!.log("- splitting fleet")
      // Modify the existing fleet to change it's number of ships
      val existingFleetNumShips = fleet.num_ships - modification.count
      star.fleets[fleetIndex] = fleet
          .num_ships(existingFleetNumShips)
          .fuel_amount(fuelFraction * (design.fuel_size * existingFleetNumShips))
          .build()

      // Add a new fleet, that's a copy of the existing fleet, but with the new number of ships.
      fleet = star.fleets[fleetIndex].newBuilder()
      star.fleets.add(fleet
          .id(identifierGenerator())
          .num_ships(modification.count.toFloat())
          .fuel_amount(fuelFraction * (design.fuel_size * modification.count))
          .build())
    } else {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to split fleet that does not exist. fleet_id=%d",
          modification.fleet_id)
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyMergeFleet(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.MERGE_FLEET)
    var fleetIndex = findFleetIndex(star, modification.fleet_id)
    if (fleetIndex >= 0) {
      val fleet = star.fleets[fleetIndex].newBuilder()
      if (fleet.state != Fleet.FLEET_STATE.IDLE) {
        // Can't merge, but this isn't particularly suspicious.
        logHandler!!.log(String.format(Locale.US,
            "  main fleet %d is %s, cannot merge.", fleet.id, fleet.state))
      }
      if (!isSameEmpire(fleet.empire_id, modification.empire_id)) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to merge fleet owned by a different empire. fleet.empire_id=%d",
            fleet.empire_id)
      }
      var i = 0
      while (i < star.fleets.size) {
        val thisFleet = star.fleets[i]
        if (thisFleet.id == fleet.id) {
          i++
          continue
        }
        if (modification.additional_fleet_ids.contains(thisFleet.id)) {
          if (thisFleet.design_type != fleet.design_type) {
            // The client shouldn't allow you to select a fleet of a different type. This would be
            // suspicious.
            throw SuspiciousModificationException(
                star.id,
                modification,
                "Fleet #%d not the same design_type as #%d (%s vs. %s)",
                thisFleet.id, fleet.id, thisFleet.design_type, fleet.design_type)
          }
          if (!isSameEmpire(thisFleet.empire_id, modification.empire_id)) {
            throw SuspiciousModificationException(
                star.id,
                modification,
                "Attempt to merge fleet owned by a different empire. fleet.empire_id=%d",
                thisFleet.empire_id)
          }
          if (thisFleet.state != Fleet.FLEET_STATE.IDLE) {
            // Again, not particularly suspicious, we'll just skip it.
            logHandler!!.log(String.format(Locale.US,
                "  fleet %d is %s, cannot merge.", thisFleet.id, thisFleet.state))
            i++
            continue
          }

          // TODO: make sure it has the same upgrades, otherwise we have to remove it.
          fleet.num_ships(fleet.num_ships + thisFleet.num_ships)
          fleet.fuel_amount(fleet.fuel_amount + thisFleet.fuel_amount)
          logHandler!!.log(String.format(Locale.US,
              "  removing fleet %d (num_ships=%.2f)", thisFleet.id, thisFleet.num_ships))

          // Remove this fleet, and keep going.
          star.fleets.removeAt(i)
          i--
        }
        i++
      }

      // fleetIndex might've changed since we've been deleting fleets.
      logHandler!!.log(String.format(Locale.US,
          "  updated fleet count of main fleet: %.2f", fleet.num_ships))
      fleetIndex = findFleetIndex(star, fleet.id)
      star.fleets[fleetIndex] = fleet.build()
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyMoveFleet(
      star: Star.Builder,
      auxStars: Collection<Star>?,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.MOVE_FLEET)
    Preconditions.checkNotNull(auxStars)
    logHandler!!.log("- moving fleet")
    var targetStar: Star? = null
    for (s in auxStars!!) {
      if (s.id == modification.star_id) {
        targetStar = s
        break
      }
    }
    if (targetStar == null) {
      // Not suspicious, the caller made a mistake not the user.
      logHandler.log(String.format(Locale.US,
          "  target star #%d was not included in the auxiliary star list.", modification.star_id))
      return
    }
    val fleetIndex = findFleetIndex(star, modification.fleet_id)
    if (fleetIndex < 0) {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to move fleet that does not exist. fleet_id=%d",
          modification.fleet_id)
    }
    val fleet = star.fleets[fleetIndex]
    if (fleet.empire_id != modification.empire_id) {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to move fleet owned by a different empire. fleet.empire_id=%d",
          fleet.empire_id)
    }
    if (fleet.state != Fleet.FLEET_STATE.IDLE) {
      // Not suspicious, maybe you accidentally pressed twice.
      logHandler.log("  fleet is not idle, can't move.")
      return
    }
    val design = getDesign(fleet.design_type)
    val distance = distanceBetween(star, targetStar)
    val timeInHours = distance / design.speed_px_per_hour
    val fuel = design.fuel_cost_per_px * distance * fleet.num_ships
    if (fleet.fuel_amount < fuel) {
      // Not enough fuel. We won't count it as suspicious, maybe a race condition.
      logHandler.log(String.format(
          Locale.US,
          "  not enough fuel in the fleet (needed %.2f, have %.2f",
          fuel, fleet.fuel_amount))
      return
    }
    logHandler.log(String.format(Locale.US, "  cost=%.2f", fuel))
    star.fleets[fleetIndex] = star.fleets[fleetIndex].newBuilder()
        .destination_star_id(targetStar.id)
        .state(Fleet.FLEET_STATE.MOVING)
        .fuel_amount(fleet.fuel_amount - fuel.toFloat())
        .state_start_time(System.currentTimeMillis())
        .eta(System.currentTimeMillis() + (timeInHours * HOURS_MS).toLong())
        .build()
  }

  private fun applyEmptyNative(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.EMPTY_NATIVE)
    logHandler!!.log("- emptying native colonies")
    for (i in star.planets.indices) {
      if (star.planets[i].colony != null
          && star.planets[i].colony.empire_id == null) {
        star.planets[i] = star.planets[i].newBuilder()
            .colony(null)
            .build()
      }
    }
    run {
      var i = 0
      while (i < star.empire_stores.size) {
        if (star.empire_stores[i].empire_id == null) {
          star.empire_stores.removeAt(i)
          i--
        }
        i++
      }
    }
    var i = 0
    while (i < star.fleets.size) {
      if (star.fleets[i].empire_id == null) {
        star.fleets.removeAt(i)
        i--
      }
      i++
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyUpgradeBuilding(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.UPGRADE_BUILDING)
    logHandler!!.log("- upgrading building")
    for (i in star.planets.indices) {
      if (star.planets[i].colony != null
          && star.planets[i].colony.id == modification.colony_id) {
        val colony = star.planets[i].colony.newBuilder()
        if (colony.empire_id != modification.empire_id) {
          throw SuspiciousModificationException(
              star.id,
              modification,
              "trying to upgrade building belonging to different empire (%d)", colony.empire_id)
        }
        var found = false
        for (j in colony.buildings.indices) {
          if (colony.buildings[j].id == modification.building_id) {
            found = true
            val building = colony.buildings[j].newBuilder()
            // TODO: check if it's at max level?
            building.level(building.level + 1)
            colony.buildings[j] = building.build()
          }
        }
        if (!found) {
          throw SuspiciousModificationException(
              star.id,
              modification,
              "trying to upgrade building that doesn't exist (%d)", modification.building_id)
        }
        star.planets[i] = star.planets[i].newBuilder().colony(colony.build()).build()
      }
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyAttackColony(
      star: Star.Builder,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.ATTACK_COLONY)
    logHandler!!.log("- attacking colony")
    var found = false
    for (i in star.planets.indices) {
      if (star.planets[i].colony != null
          && star.planets[i].colony.id == modification.colony_id) {
        val colony = star.planets[i].colony.newBuilder()
        if (colony.empire_id != null && colony.empire_id == modification.empire_id) {
          // Not suspicious, just dumb...
          logHandler.log("- trying to attack your own colony, ignoring.")
          return
        }
        found = true
        for (j in star.fleets.indices) {
          val fleet = star.fleets[j]
          if (isOwnedBy(fleet, modification.empire_id) && fleet.design_type == Design.DesignType.TROOP_CARRIER) {
            // It's our troop carrier, so we reduce the colony's population by the requisite amount
            val numShips = fleet.num_ships / colony.defence_bonus
            if (colony.population < numShips) {
              val fraction = colony.population / numShips

              // We've destroyed the colony. Figure out how many ships are left, and update the
              // fleet according. Also, remove the colony itself.
              star.planets[i] = star.planets[i].newBuilder().colony(null).build()
              star.fleets[j] = fleet.newBuilder()
                  .num_ships(fleet.num_ships - fleet.num_ships * fraction)
                  .build()
            } else {
              // We haven't destroyed the colony yet, but this fleet is destroyed.
              star.planets[i] = star.planets[i].newBuilder()
                  .colony(colony.population(colony.population - numShips).build())
                  .build()
              star.fleets[j] = fleet.newBuilder().num_ships(0f).build()
            }
          }
        }
      }
    }
    if (!found) {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "trying to attack a colony that doesn't exist (%d)", modification.colony_id)
    }
  }

  private fun getPlanetWithColony(star: Star.Builder, colonyId: Long): Planet? {
    for (i in star.planets.indices) {
      val planet = star.planets[i]
      if (planet.colony != null && planet.colony.id == colonyId) {
        return planet
      }
    }
    return null
  }

  private fun findFleetIndex(star: Star.Builder, fleetId: Long): Int {
    for (i in star.fleets.indices) {
      if (star.fleets[i].id == fleetId) {
        return i
      }
    }
    return -1
  }

  companion object {
    private val log = Log("StarModifier")
    private const val HOURS_MS = 3600000L

    val EMPTY_LOG_HANDLER: Simulation.LogHandler = object : Simulation.LogHandler {
      override fun setStarName(starName: String?) {}
      override fun log(message: String) {}
    }
  }
}