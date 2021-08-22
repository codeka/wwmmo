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
import java.util.function.Predicate
import kotlin.math.ceil
import kotlin.math.max

/** Class for handling modifications to a star. */
class StarModifier(private val identifierGenerator: () -> Long) {
  /**
   * Modify a star, and possibly other auxiliary stars.
   */
  fun modifyStar(
      star: MutableStar,
      modification: StarModification,
      auxStars: Collection<Star>? = null,
      sitReports: MutableMap<Long, SituationReport>? = null,
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
      star: MutableStar,
      modifications: Collection<StarModification>,
      auxStars: Collection<Star>? = null,
      sitReports: MutableMap<Long, SituationReport>? = null,
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
      star: MutableStar,
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
      StarModification.MODIFICATION_TYPE.MOVE_FLEET -> applyMoveFleet(star, auxStars!!, modification, logHandler)
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
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.COLONIZE)
    logHandler!!.log(String.format(Locale.US, "- colonizing planet #%d", modification.planet_index))

    // When we destroy the colonyship, we'll take it's energy and add it to the star's supply.
    var remainingFuel = 0f

    // Destroy a colony ship, unless this is a native colony.
    if (modification.empire_id != 0L) {
      var found = false
      for (i in star.fleets.indices) {
        val fleet = star.fleets[i]
        if (fleet.designType == Design.DesignType.COLONY_SHIP &&
            fleet.empireId == modification.empire_id &&
            fleet.state == Fleet.FLEET_STATE.IDLE) {
          // TODO: check for cryogenics
          if (ceil(fleet.numShips.toDouble()) == 1.0) {
            remainingFuel = star.fleets[i].fuelAmount
            star.fleets.removeAt(i)
          } else {
            // Make sure we don't have too much fuel.
            val design = getDesign(fleet.designType)
            val maxFuelAmount = design.fuel_size!! * (fleet.numShips - 1)
            val fuelAmount = max(fleet.fuelAmount, maxFuelAmount)
            remainingFuel = fleet.fuelAmount - fuelAmount
            star.fleets[i].numShips --
            star.fleets[i].fuelAmount = fuelAmount
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
    star.planets[modification.planet_index!!].colony = MutableColony(Colony(
            cooldown_end_time = System.currentTimeMillis() + 15 * Time.MINUTE,
            empire_id = modification.empire_id,
            focus = ColonyFocus(
                construction = 0.1f,
                energy = 0.3f,
                farming = 0.3f,
                mining = 0.3f),
            id = identifierGenerator(),
            population = 100.0f,
            defence_bonus = 1.0f))
    logHandler.log(String.format(Locale.US,
        "  colonized: colony_id=%d",
        star.planets[modification.planet_index].colony?.id))

    // if there's no storage for this empire, add one with some defaults now (only for non-natives)
    val empireId = modification.empire_id
    if (empireId != null) {
      val storageIndex = getStorageIndex(star, empireId)
      if (storageIndex < 0) {
        val storage = createDefaultStorage(empireId)
        storage.totalEnergy += remainingFuel
        star.empireStores.add(storage)
      } else {
        val storage = star.empireStores[storageIndex]
        storage.totalEnergy += remainingFuel
      }
    }
  }

  private fun createDefaultStorage(empireId: Long): MutableEmpireStorage {
    return MutableEmpireStorage(EmpireStorage(
        empire_id = empireId,
        total_goods = 100.0f,
        total_minerals = 25000f,
        total_energy = 20000f,
        max_goods = 200.0f,
        max_minerals = 5000.0f,
        max_energy = 5000.0f))
  }

  private fun applyCreateFleet(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.CREATE_FLEET)
    var attack = false
    if (modification.fleet == null || modification.fleet.stance == Fleet.FLEET_STANCE.AGGRESSIVE) {
      for (fleet in star.fleets) {
        if (!isFriendly(fleet.proto, modification.empire_id)) {
          attack = true
        }
      }
    }

    // First, if there's any fleets that are not friendly and aggressive, they should change to
    // attacking as well.
    var numAttacking = 0
    for (i in star.fleets.indices) {
      val fleet = star.fleets[i]
      if (!isFriendly(fleet.proto, modification.empire_id)
        && fleet.stance == Fleet.FLEET_STANCE.AGGRESSIVE) {
        fleet.state = Fleet.FLEET_STATE.ATTACKING
        fleet.stateStartTime = System.currentTimeMillis()
        numAttacking++
      }
    }
    var fuelAmount: Float
    val design = getDesign(
        if (modification.fleet == null) modification.design_type!!
        else modification.fleet.design_type)
    val numShips =
        if (modification.fleet == null) modification.count!!.toFloat()
        else modification.fleet.num_ships
    if (modification.full_fuel != null && modification.full_fuel) {
      fuelAmount = design.fuel_size!! * numShips
    } else {
      // It'll be refueled when we simulate, if there's any energy on the star.
      fuelAmount = 0f
      if (modification.fleet != null) {
        fuelAmount = modification.fleet.fuel_amount ?: 0f
      }
    }

    // Now add the fleet itself.
    logHandler!!.log(String.format(Locale.US, "- creating fleet (%s) numAttacking=%d fuel=%.2f",
        if (attack) "attacking" else "not attacking",
        numAttacking, fuelAmount))
    if (modification.fleet != null) {
      val fleet = MutableFleet(modification.fleet)
      fleet.id = identifierGenerator()
      fleet.state = if (attack) Fleet.FLEET_STATE.ATTACKING else Fleet.FLEET_STATE.IDLE
      fleet.stateStartTime = System.currentTimeMillis()
      fleet.fuelAmount = fuelAmount
      fleet.destinationStarId = null
      fleet.eta = null
      star.fleets.add(fleet)

      // If there's an existing fleet, and it has a SCOUT_SHIP effect, then generate a scout report.
      if (hasEffect(modification.fleet, Design.EffectType.SCOUT_SHIP)) {
        generateScoutReport(star, modification)
      }
    } else {
      val fleet = MutableFleet(Fleet(
        design_type = modification.design_type!!,
        empire_id = modification.empire_id,
        id = identifierGenerator(),
        num_ships = modification.count!!.toFloat(),
        fuel_amount = fuelAmount,
        stance = Fleet.FLEET_STANCE.AGGRESSIVE,
        state = if (attack) Fleet.FLEET_STATE.ATTACKING else Fleet.FLEET_STATE.IDLE,
        state_start_time = System.currentTimeMillis()))
      star.fleets.add(fleet)
    }
  }

  private fun generateScoutReport(star: MutableStar, modification: StarModification) {
    val fleets = ArrayList<Fleet>()
    val planets = ArrayList<Planet>()
    for (fleet in star.fleets) {
      // Skip fleets that are moving, they're technically not "on" this star.
      if (fleet.state == Fleet.FLEET_STATE.MOVING) {
        continue
      }
      val design = getDesign(fleet.designType)
      // We'll put the fuel as max so even with a scout report, you can't tell.
      fleets.add(fleet.build().copy(
        fuel_amount = (design.fuel_size!! * fleet.numShips)))
    }
    for (p in star.planets) {
      var colony: Colony? = null
      if (p.colony != null) {
        // Rebuild the colony with only the things we want.
        colony = (Colony(
            id = p.colony!!.id,
            empire_id = p.colony!!.empireId,
            population = p.colony!!.population,
            focus = p.colony!!.focus.build()))
      }
      planets.add(p.build().copy(colony = colony))
    }

    // Now check whether there's already a report for this empire and if there is we'll just replace
    // that one with this one.
    val scoutReport = ScoutReport(
      report_time = System.currentTimeMillis(), // TODO: record a better time
      star_id = star.id,
      empire_id = modification.empire_id,
      fleets = fleets,
      planets = planets)

    var existingReport = false
    for (i in star.scoutReports.indices) {
      if (isSameEmpire(star.scoutReports[i].empire_id, modification.empire_id)) {
        existingReport = true
        star.scoutReports[i] = scoutReport
        break
      }
    }
    if (!existingReport) {
      star.scoutReports.add(scoutReport)
    }
  }

  @Throws(SuspiciousModificationException::class)
  private fun applyCreateBuilding(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.CREATE_BUILDING)
    val planet = getPlanetWithColony(star, modification.colony_id!!)
    if (planet != null) {
      val colony = planet.colony!! // TODO: suss
      if (!isSameEmpire(colony.empireId, modification.empire_id)) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to create building on planet for different empire. colony.empire_id=%d",
            colony.empireId)
      }
      logHandler.log("- creating building, colony_id=${modification.colony_id}")
      colony.buildings.add(MutableBuilding(Building(
          id = identifierGenerator(),
          design_type = modification.design_type!!,
          level = 1)))
      planet.colony = colony
    } else {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to create building on colony that does not exist. colony_id=%d",
          modification.colony_id)
    }
  }

  /**
   * Returns the colony associated with the given [StarModification], or throws a
   * [SuspiciousModificationException] if the colony doesn't exist.
   */
  private fun getColony(star: MutableStar, modification: StarModification): MutableColony {
    val colonyId = modification.colony_id
      ?: throw SuspiciousModificationException(star.id, modification, "colony_id is null")

    val planet = getPlanetWithColony(star, colonyId)
      ?: throw SuspiciousModificationException(
        star.id, modification,
        "Attempt to adjust focus on a colony that does not exist. colony_id=%d",
        modification.colony_id)

    val colony = planet.colony!! // should be non-null since getPlanetWithColony returned it.
    if (!isSameEmpire(colony.empireId, modification.empire_id)) {
      throw SuspiciousModificationException(
        star.id,
        modification,
        "Attempt to adjust focus on planet for different empire. colony.empire_id=%d",
        colony.empireId)
    }

    return colony
  }

  private fun applyAdjustFocus(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(
      modification.type == StarModification.MODIFICATION_TYPE.ADJUST_FOCUS)
    val colony = getColony(star, modification)

    // TODO: make sure the focus is valid (i.e. all adds up to 1)

    logHandler.log("- adjusting focus.")
    colony.focus = MutableFocus(modification.focus!!)
  }

  private fun applyAddBuildRequest(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(
        modification.type == StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)

    val colony = getColony(star, modification)

    // If the build request is for a ship and this colony doesn't have a shipyard, you can't
    // build. That's suspicious.
    val design = getDesign(modification.design_type!!)
    if (design.design_kind == Design.DesignKind.SHIP) {
      var hasShipyard = false
      for (building in colony.buildings) {
        if (building.designType == Design.DesignType.SHIPYARD) {
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
      var building: MutableBuilding? = null
      for (bldg in colony.buildings) {
        if (bldg.id == modification.building_id) {
          building = bldg
        }
      }
      if (building == null) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Cannot upgrade a building that doesn't exist: #%d", modification.building_id)
      }

      // If the modification specifies a different design to the building, that's suspicious.
      if (building.designType != design.type) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Cannot upgrade a building to a different design, " +
                "current=${building.designType} new=${modification.design_type}")
      }

      // And it shouldn't be for a higher level than the building supports
      if (building.level > design.upgrades.size) {
        throw SuspiciousModificationException(
            star.id,
            modification,
            "Cannot upgrade a building past it's maximum level, ${design.upgrades.size + 1}")
      }
    }
    var count = 1
    if (design.design_kind == Design.DesignKind.SHIP) {
      count = modification.count!!
    }
    logHandler.log("- adding build request")
    colony.buildRequests.add(MutableBuildRequest(BuildRequest(
        id = identifierGenerator(),
        design_type = modification.design_type,
        building_id = modification.building_id,
        start_time = System.currentTimeMillis(),
        count = count,
        progress = 0.0f)))
  }

  private fun applyDeleteBuildRequest(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(
        modification.type == StarModification.MODIFICATION_TYPE.DELETE_BUILD_REQUEST)
    var planet: MutablePlanet? = null
    var buildRequest: MutableBuildRequest? = null
    for (p in star.planets) {
      val colony = p.colony
      if (colony != null) {
        for (br in colony.buildRequests) {
          if (br.id == modification.build_request_id) {
            if (!isSameEmpire(colony.empireId, modification.empire_id)) {
              throw SuspiciousModificationException(
                  star.id,
                  modification,
                  "Attempt to delete build request for different empire. building.empire_id=%d",
                  colony.empireId)
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
    if (planet == null || buildRequest == null) {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to delete build request that does not exist. build_request_id=%d",
          modification.build_request_id)
    }
    val idToDelete = buildRequest.id

    logHandler.log("- deleting build request")
    val colony = planet.colony!!
    colony.buildRequests = ArrayList(colony.buildRequests.filter { it.id != idToDelete })
  }

  private fun getFleet(star: MutableStar, modification: StarModification): MutableFleet {
    val fleet = star.fleets.find { it.id == modification.fleet_id!! }
      ?: throw SuspiciousModificationException(
        star.id, modification, "Attempt to split fleet that does not exist. fleet_id=%d",
        modification.fleet_id)

    if (!isSameEmpire(fleet.empireId, modification.empire_id)) {
      throw SuspiciousModificationException(
        star.id,
        modification,
        "Attempt to split fleet of different empire. fleet.empire_id=%d",
        fleet.empireId)
    }

    return fleet
  }

  private fun applySplitFleet(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler?) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.SPLIT_FLEET)

    val fleet = getFleet(star, modification)
    val design = getDesign(fleet.designType)
    val fuelFraction = fleet.fuelAmount / (design.fuel_size!! * fleet.numShips)
    logHandler!!.log("- splitting fleet")
    // Modify the existing fleet to change it's number of ships
    val existingFleetNumShips = fleet.numShips - modification.count!!

    fleet.numShips = existingFleetNumShips
    fleet.fuelAmount = fuelFraction * (design.fuel_size * existingFleetNumShips)

    // Add a new fleet, that's a copy of the existing fleet, but with the new number of ships.
    star.fleets.add(MutableFleet(fleet.build().copy(
        id = identifierGenerator(),
        num_ships = modification.count.toFloat(),
        fuel_amount = fuelFraction * (design.fuel_size * modification.count))))
  }

  private fun applyMergeFleet(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.MERGE_FLEET)

    val fleet = getFleet(star, modification)
    if (fleet.state != Fleet.FLEET_STATE.IDLE) {
      // Can't merge, but this isn't particularly suspicious.
      logHandler.log(String.format(Locale.US,
          "  main fleet %d is %s, cannot merge.", fleet.id, fleet.state))
    }

    var i = 0
    while (i < star.fleets.size) {
      val thisFleet = star.fleets[i]
      if (thisFleet.id == fleet.id) {
        i++
        continue
      }
      if (modification.additional_fleet_ids.contains(thisFleet.id)) {
        if (thisFleet.designType != fleet.designType) {
          // The client shouldn't allow you to select a fleet of a different type. This would be
          // suspicious.
          throw SuspiciousModificationException(
              star.id,
              modification,
              "Fleet #%d not the same design_type as #%d (%s vs. %s)",
              thisFleet.id, fleet.id, thisFleet.designType, fleet.designType)
        }
        if (!isSameEmpire(thisFleet.empireId, modification.empire_id)) {
          throw SuspiciousModificationException(
              star.id,
              modification,
              "Attempt to merge fleet owned by a different empire. fleet.empire_id=%d",
              thisFleet.empireId)
        }
        if (thisFleet.state != Fleet.FLEET_STATE.IDLE) {
          // Again, not particularly suspicious, we'll just skip it.
          logHandler.log(String.format(Locale.US,
              "  fleet %d is %s, cannot merge.", thisFleet.id, thisFleet.state))
          i++
          continue
        }

        // TODO: make sure it has the same upgrades, otherwise we have to remove it.
        fleet.numShips = fleet.numShips + thisFleet.numShips
        fleet.fuelAmount = fleet.fuelAmount + thisFleet.fuelAmount
        logHandler.log(String.format(Locale.US,
            "  removing fleet %d (num_ships=%.2f)", thisFleet.id, thisFleet.numShips))

        // Remove this fleet, and keep going.
        star.fleets.removeAt(i)
        i--
      }
      i++
    }

    // fleetIndex might've changed since we've been deleting fleets.
    logHandler.log(String.format(Locale.US,
        "  updated fleet count of main fleet: %.2f", fleet.numShips))
  }

  private fun applyMoveFleet(
      star: MutableStar,
      auxStars: Collection<Star>,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.MOVE_FLEET)
    logHandler.log("- moving fleet")
    var targetStar: Star? = null
    for (s in auxStars) {
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

    val fleet = getFleet(star, modification)
    if (fleet.state != Fleet.FLEET_STATE.IDLE) {
      // Not suspicious, maybe you accidentally pressed twice.
      logHandler.log("  fleet is not idle, can't move.")
      return
    }
    val design = getDesign(fleet.designType)
    val distance = distanceBetween(star, targetStar)
    val timeInHours = distance / design.speed_px_per_hour!!
    val fuel = design.fuel_cost_per_px!! * distance * fleet.numShips
    if (fleet.fuelAmount < fuel) {
      // Not enough fuel. We won't count it as suspicious, maybe a race condition.
      logHandler.log(String.format(
          Locale.US,
          "  not enough fuel in the fleet (needed %.2f, have %.2f",
          fuel, fleet.fuelAmount))
      return
    }
    logHandler.log(String.format(Locale.US, "  cost=%.2f", fuel))
    fleet.destinationStarId = targetStar.id
    fleet.state = Fleet.FLEET_STATE.MOVING
    fleet.fuelAmount = fleet.fuelAmount - fuel.toFloat()
    fleet.stateStartTime = System.currentTimeMillis()
    fleet.eta = System.currentTimeMillis() + (timeInHours * HOURS_MS).toLong()
  }

  private fun applyEmptyNative(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(
      modification.type == StarModification.MODIFICATION_TYPE.EMPTY_NATIVE)
    logHandler.log("- emptying native colonies")

    for (planet in star.planets) {
      val colony = planet.colony ?: continue
      if (colony.empireId == null) {
        planet.colony = null
      }
    }

    star.empireStores = ArrayList(star.empireStores.filter { it.empireId != 0L })
    star.fleets = ArrayList(star.fleets.filter { it.empireId != null })
  }

  private fun applyUpgradeBuilding(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(
        modification.type == StarModification.MODIFICATION_TYPE.UPGRADE_BUILDING)

    val colony = getColony(star, modification)
    logHandler.log("- upgrading building")
    var found = false
    for (j in colony.buildings.indices) {
      if (colony.buildings[j].id == modification.building_id) {
        found = true
        val building = colony.buildings[j]
        val design = getDesign(building.designType)
        if (building.level > design.upgrades.size) {
          // We don't throw an exception here, just ignore it. The error should have been
          // caught by the addBuildRequest modification.
          logHandler.log(
              "  - cannot upgrade, already too high level (" +
                  "${design.upgrades.size} < ${building.level})")
          return
        }

        building.level = building.level + 1
      }
    }
    if (!found) {
      throw SuspiciousModificationException(
          star.id,
          modification,
          "trying to upgrade building that doesn't exist (%d)", modification.building_id)
    }
  }

  private fun applyAttackColony(
      star: MutableStar,
      modification: StarModification,
      logHandler: Simulation.LogHandler) {
    Preconditions.checkArgument(modification.type == StarModification.MODIFICATION_TYPE.ATTACK_COLONY)
    logHandler.log("- attacking colony")
    var found = false
    for (planet in star.planets) {
      val colony = planet.colony
      if (colony != null && colony.id == modification.colony_id!!) {
        if (colony.empireId != null && colony.empireId == modification.empire_id) {
          // Not suspicious, just dumb...
          logHandler.log("- trying to attack your own colony, ignoring.")
          return
        }
        found = true
        for (fleet in star.fleets) {
          if (isOwnedBy(fleet.proto, modification.empire_id)
            && fleet.designType == Design.DesignType.TROOP_CARRIER) {
            // It's our troop carrier, so we reduce the colony's population by the requisite amount
            val numShips = fleet.numShips / colony.defenceBonus
            if (colony.population < numShips) {
              val fraction = colony.population / numShips

              // We've destroyed the colony. Figure out how many ships are left, and update the
              // fleet according. Also, remove the colony itself.
              planet.colony = null
              fleet.numShips -= fleet.numShips * fraction
            } else {
              // We haven't destroyed the colony yet, but this fleet is destroyed.
              colony.population -= numShips
              fleet.numShips = 0f
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

  private fun getPlanetWithColony(star: MutableStar, colonyId: Long): MutablePlanet? {
    for (i in star.planets.indices) {
      val planet = star.planets[i]
      val colony = planet.colony
      if (colony != null && colony.id == colonyId) {
        return planet
      }
    }
    return null
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