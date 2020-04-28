package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Time
import au.com.codeka.warworlds.common.Time.toHours
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.sim.ColonyHelper.getMaxPopulation
import au.com.codeka.warworlds.common.sim.DesignHelper.getDesign
import au.com.codeka.warworlds.common.sim.EmpireHelper.getStore
import au.com.codeka.warworlds.common.sim.EmpireHelper.getStoreIndex
import au.com.codeka.warworlds.common.sim.FleetHelper.hasEffect
import au.com.codeka.warworlds.common.sim.FleetHelper.isFriendly
import au.com.codeka.warworlds.common.sim.SimulationHelper.isInvalid
import au.com.codeka.warworlds.common.sim.SimulationHelper.trimTimeToStep
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

/** This class is used to simulate a [Star].  */
class Simulation @JvmOverloads constructor(
    private val timeOverride: Long = System.currentTimeMillis(),
    private val predict: Boolean = true,
    private val logHandler: LogHandler? = if (sDebug) BasicLogHandler() else null) {

  constructor(log: LogHandler?)
      : this(System.currentTimeMillis(), true, log)
  constructor(predict: Boolean)
      : this(System.currentTimeMillis(), predict, if (sDebug) BasicLogHandler() else null)
  constructor(timeOverride: Long, log: LogHandler?)
      : this(timeOverride, true, log)

  /**
   * Simulate the given star, and make sure it's "current".
   *
   * @param star The [Star.Builder] of the star to simulate. We modify the builder in-place
   * with the new values.
   */
  fun simulate(
      star: Star.Builder,
      sitReports: MutableMap<Long, SituationReport.Builder>? = null) {
    logHandler?.setStarName(star.name)
    log("Begin simulation for '${star.name}' @ $timeOverride")

    // figure out the start time, which is the oldest last_simulation time
    val startTime = getSimulateStartTime(star)
    val endTime = trimTimeToStep(timeOverride)
    val empireIds = HashSet<Long?>()
    for (planet in star.planets) {
      if (planet.colony != null) {
        empireIds.add(planet.colony.empire_id)
      }
    }

    // We'll simulate in "prediction mode" for an extra bit of time so that we can get a more
    // accurate estimate of the end time for builds. We won't *record* the population growth and
    // such, just the end time of builds. We'll also record the time that the population drops below
    // a certain threshold so that we can warn the player.
    val predictionTime = endTime + Time.DAY
    var predictionStar: Star.Builder? = null
    var now = startTime
    while (true) {
      if (now <= endTime) {
        simulateStepForAllEmpires(now, star, empireIds)
      } else if (predictionStar == null) {
        // This is also the time to simulate combat. The star has been simulated up to "now", combat
        // can run, and then we can do the first prediction once combat has completed.
        simulateCombat(star, now, sitReports)

        // Also, this is the time to empty any tankers. They can be refilled if you have energy
        // production on this star.
        simulateEnergyTransports(star, now)

        // We always predict at least one more step, so that we can put the deltas from the next
        // step in (since they'll take into account things like focus changes, new builds, etc that
        // the user has applied in THIS step).
        predictionStar = star.build().newBuilder()
        log("Begin prediction")
        simulateStepForAllEmpires(now, predictionStar, empireIds)
        copyDeltas(star, predictionStar)
      } else if (predict && now <= predictionTime) {
        simulateStepForAllEmpires(now, predictionStar, empireIds)
      } else {
        break
      }
      now += STEP_TIME
    }

    // Copy the end times for builds from the prediction star.
    val stepStartTime = trimTimeToStep(timeOverride)
    for (i in star.planets.indices) {
      val predictionPlanet = predictionStar!!.planets[i]
      val planet = star.planets[i].newBuilder()
      if (planet.colony == null || predictionPlanet.colony == null ||
          planet.colony.build_requests == null || predictionPlanet.colony.build_requests == null) {
        continue
      }
      val buildRequests = ArrayList<BuildRequest>()
      for (predictionBuildRequest in predictionPlanet.colony.build_requests) {
        for (j in planet.colony.build_requests.indices) {
          val br = planet.colony.build_requests[j].newBuilder()
          if (predictionBuildRequest.id == br.id) {
            br.end_time(predictionBuildRequest.end_time)
            buildRequests.add(br.build())
          }
        }
      }
      planet.colony(planet.colony.newBuilder().build_requests(buildRequests).build())
      star.planets[i] = planet.build()
    }
    star.last_simulation = timeOverride
  }

  /**
   * After simulating the first step in the prediction star, copy the mineral, goods and energy
   * deltas across to the main star. Also copy the build request efficiencies, since they're also
   * the ones we'll care about (i.e. the efficient for the *next* step).
   */
  private fun copyDeltas(star: Star.Builder, predictionStar: Star.Builder) {
    val stores = ArrayList<EmpireStorage>()
    for (i in star.empire_stores.indices) {
      val predictionStore = getStore(predictionStar, star.empire_stores[i].empire_id)
          ?: // The empire has been wiped out or something, just ignore.
          continue
      stores.add(star.empire_stores[i].newBuilder()
          .goods_delta_per_hour(predictionStore.goods_delta_per_hour)
          .minerals_delta_per_hour(predictionStore.minerals_delta_per_hour)
          .energy_delta_per_hour(predictionStore.energy_delta_per_hour)
          .build())
    }
    star.empire_stores(stores)
    for (i in star.planets.indices) {
      val planetBuilder = star.planets[i].newBuilder()
      if (planetBuilder.colony != null) {
        val colonyBuilder = planetBuilder.colony.newBuilder()
        for (j in planetBuilder.colony.build_requests.indices) {
          val brBuilder = colonyBuilder.build_requests[j].newBuilder()
          val predictionBuildRequest = predictionStar.planets[i].colony.build_requests[j]
          brBuilder.minerals_efficiency(predictionBuildRequest.minerals_efficiency)
          brBuilder.population_efficiency(predictionBuildRequest.population_efficiency)
          brBuilder.progress_per_step(predictionBuildRequest.progress_per_step)
          brBuilder.delta_minerals_per_hour(predictionBuildRequest.delta_minerals_per_hour)
          colonyBuilder.build_requests[j] = brBuilder.build()
        }
        planetBuilder.colony(colonyBuilder.build())
      }
      star.planets[i] = planetBuilder.build()
    }
  }

  /**
   * Gets the time we should start simulating this star for.
   *
   * Most of the time, this will be the time the star was last simulated, but in the case of native-
   * only stars, we'll just simulate the last 24 hours.
   */
  private fun getSimulateStartTime(star: Star.Builder): Long {
    var lastSimulation = star.last_simulation

    // if there's only native colonies, don't bother simulating from more than
    // 24 hours ago. The native colonies will generally be in a steady state
    val oneDayAgo = timeOverride - Time.DAY
    if (lastSimulation != null && lastSimulation < oneDayAgo) {
      log("Last simulation more than on day ago, checking whether there are any non-native colonies.")
      var onlyNativeColonies = true
      for (planet in star.planets) {
        if (planet.colony?.empire_id != null) {
          onlyNativeColonies = false
          break
        }
      }
      for (fleet in star.fleets) {
        if (fleet.empire_id != null) {
          onlyNativeColonies = false
          break
        }
      }
      if (onlyNativeColonies) {
        log("No non-native colonies detected, simulating only 24 hours in the past.")
        lastSimulation = oneDayAgo
      }
    }
    if (lastSimulation == null) {
      log("Star has never been simulated, simulating for 1 step only")
      return trimTimeToStep(timeOverride)
    }
    return trimTimeToStep(lastSimulation) + STEP_TIME
  }

  private fun simulateStepForAllEmpires(now: Long, star: Star.Builder, empireIds: Set<Long?>) {
    log("- Step [now=%d]", now)
    for (empireId in empireIds) {
      log(String.format("-- Empire [%s]", empireId ?: "Native"))
      simulateStep(now, star, empireId)
    }

    // Remove stores for any empires that don't exist anymore.
    var i = 0
    while (i < star.empire_stores.size) {
      if (!empireIds.contains(star.empire_stores[i].empire_id)) {
        star.empire_stores.removeAt(i)
        i--
      }
      i++
    }
  }

  private fun simulateStep(now: Long, star: Star.Builder, empireId: Long?) {
    var totalPopulation = 0.0f
    val storageIndex = getStoreIndex(star, empireId)
    if (storageIndex < 0) {
      log("No storage found for this empire!")
      return
    }
    val storage = star.empire_stores[storageIndex].newBuilder()
    storage.max_goods = 200.0f
    storage.max_energy = 5000.0f
    storage.max_minerals = 5000.0f
    val dt = toHours(STEP_TIME)
    var goodsDeltaPerHour = 0.0f
    var mineralsDeltaPerHour = 0.0f
    var energyDeltaPerHour = 0.0f
    for (i in star.planets.indices) {
      val planet = star.planets[i]
      if (planet.colony == null) {
        continue
      }
      val colony = planet.colony.newBuilder()
      if (!equalEmpire(colony.empire_id, empireId)) {
        continue
      }

      // Apply storage bonuses this round (note: this could change from step to step, if a building
      // finishes being built, for example).
      for (building in colony.buildings) {
        val design = getDesign(building.design_type)
        var effects = design.effect
        if (building.level != null && building.level > 1) {
          // Level will be 2 for the first upgrade, 3 for the second and so on.
          effects = design.upgrades[building.level - 2].effects
        }
        for (effect in effects) {
          if (effect.type == Design.EffectType.STORAGE) {
            storage.max_goods += effect.goods.toFloat()
            storage.max_minerals += effect.minerals.toFloat()
            storage.max_energy += effect.energy.toFloat()
          }
        }
      }

      // Some sanity checks.
      if (isInvalid(storage.total_energy)) {
        storage.total_energy = 0.0f
      }
      if (isInvalid(storage.total_goods)) {
        storage.total_goods = 0.0f
      }
      if (isInvalid(storage.total_minerals)) {
        storage.total_minerals = 0.0f
      }
      if (isInvalid(colony.focus.construction) ||
          isInvalid(colony.focus.farming) ||
          isInvalid(colony.focus.mining) ||
          isInvalid(colony.focus.energy)) {
        colony.focus(colony.focus.newBuilder()
            .construction(0.25f).energy(0.25f).farming(0.25f).mining(0.25f).build())
      }
      log("--- Colony [planetIndex=%d] [population=%.2f]", i, colony.population)

      // Calculate the output from farming this turn and add it to the star global
      val goods = colony.population * colony.focus.farming * (planet.farming_congeniality / 100.0f)
      colony.delta_goods(goods)
      storage.total_goods(0f.coerceAtLeast(storage.total_goods + goods * dt))
      goodsDeltaPerHour += goods
      log("    Goods: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_goods, goods, goods * dt)

      // calculate the output from mining this turn and add it to the star global
      val mineralsPerHour = colony.population * colony.focus.mining * (planet.mining_congeniality / 10.0f)
      colony.delta_minerals(mineralsPerHour)
      storage.total_minerals(0f.coerceAtLeast(storage.total_minerals + mineralsPerHour * dt))
      mineralsDeltaPerHour += mineralsPerHour
      log("    Minerals: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_minerals, mineralsPerHour, mineralsPerHour * dt)

      // calculate the output from energy this turn and add it to the star global
      val energy = colony.population * colony.focus.energy * (planet.energy_congeniality / 10.0f)
      colony.delta_energy(energy)
      storage.total_energy(0f.coerceAtLeast(storage.total_energy + energy * dt))
      energyDeltaPerHour += energy
      log("    Energy: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_energy, energy, energy * dt)
      totalPopulation += colony.population
      star.planets[i] = planet.newBuilder().colony(colony.build()).build()
    }

    // A second loop though the colonies, once the goods/minerals/energy has been calculated.
    for (i in star.planets.indices) {
      val planet = star.planets[i].newBuilder()
      if (planet.colony == null) {
        continue
      }
      val colony = planet.colony.newBuilder()
      if (!equalEmpire(colony.empire_id, empireId)) {
        continue
      }
      if (colony.build_requests == null || colony.build_requests.isEmpty()) {
        continue
      }

      // not all build requests will be processed this turn. We divide up the population
      // based on the number of ACTUAL build requests they'll be working on this turn
      var numValidBuildRequests = 0
      for (br in colony.build_requests) {
        if (br.start_time >= now) {
          continue
        }
        if (br.progress >= 1.0f) {
          continue
        }

        // as long as it's started but hasn't finished, we'll be working on it this turn
        numValidBuildRequests += 1
      }

      // If we have pending build requests, we'll have to update them as well
      if (numValidBuildRequests > 0) {
        val totalWorkers = colony.population * colony.focus.construction
        var workersPerBuildRequest = totalWorkers / numValidBuildRequests
        val mineralsPerBuildRequest = storage.total_minerals / numValidBuildRequests
        log("--- Building [buildRequests=%d] [planetIndex=%d] [totalWorker=%.2f] [totalMinerals=%.2f]",
            numValidBuildRequests, planet.index, totalWorkers, storage.total_minerals)

        // OK, we can spare at least ONE population
        if (workersPerBuildRequest < 1.0f) {
          workersPerBuildRequest = 1.0f
        }
        val completeBuildRequests = ArrayList<BuildRequest>()
        for (j in colony.build_requests.indices) {
          val br = colony.build_requests[j].newBuilder()
          // Sanity check.
          if (isInvalid(br.progress)) {
            br.progress(0.0f)
          }
          val design = getDesign(br.design_type)
          val startTime = br.start_time
          if (startTime > now || br.progress >= 1.0f) {
            completeBuildRequests.add(br.build())
            continue
          }

          // the build cost is defined by the original design, or possibly by the upgrade if that
          // is what it is.
          var buildCost = design.build_cost
          if (br.building_id != null) {
            for (building in colony.buildings) {
              if (building.id == br.building_id) {
                buildCost = design.upgrades[building.level - 1].build_cost
                break
              }
            }
          }
          //if (br.mExistingFleetID != null) {
          //  ShipDesign shipDesign = (ShipDesign) design;
          //  ShipDesign.Upgrade upgrade = shipDesign.getUpgrade(br.getUpgradeID());
          //  buildCost = upgrade.getBuildCost();
          //}
          log("---- Building [design=%s %s] [count=%d] cost [workers=%d] [minerals=%d] [start-time=%d]",
              design.design_kind, design.type, br.count, buildCost.population, buildCost.minerals,
              br.start_time)

          // The total amount of time to build something is based on the number of workers it
          // requires, if you have the right number of workers and the right amount of minerals,
          // you can finish the build in one turn. However, if you only have a fraction of them
          // available, then that fraction of progress will be made.
          val totalWorkersRequired = buildCost.population * br.count.toFloat()
          val totalMineralsRequired = buildCost.minerals * br.count.toFloat()
          log("     Required: [population=%.2f] [minerals=%.2f]",
              totalWorkersRequired, totalMineralsRequired)
          log("     Available: [population=%.2f] [minerals=%.2f]",
              workersPerBuildRequest, mineralsPerBuildRequest)

          // The amount of work we can do this turn is based on how much population we have (if
          // we have enough minerals) or based the min of population/minerals if we don't.
          val populationProgressThisTurn = workersPerBuildRequest / totalWorkersRequired
          val mineralsProgressThisTurn = mineralsPerBuildRequest / totalMineralsRequired
          var progressThisTurn =
              if (mineralsProgressThisTurn >= 1.0) populationProgressThisTurn
              else populationProgressThisTurn.coerceAtMost(mineralsProgressThisTurn)
          log("     Progress: [this turn=%.4f (minerals=%.4f pop=%.4f] [total=%.4f]",
              progressThisTurn,
              mineralsProgressThisTurn,
              populationProgressThisTurn,
              br.progress + progressThisTurn)

          // If it started half way through this step, the progress is lessened.
          if (br.start_time > now) {
            val fraction = (startTime.toFloat() - now) / STEP_TIME
            progressThisTurn *= fraction
            log("     Reduced progress: %.2f (fraction=%.2f)", progressThisTurn, fraction)
          }
          var mineralsUsedThisTurn = totalMineralsRequired.coerceAtMost(mineralsPerBuildRequest)
          if (populationProgressThisTurn < mineralsProgressThisTurn) {
            // If we're limited by population, then we won't have used all of the minerals that
            // were available to us.
            mineralsUsedThisTurn *= populationProgressThisTurn
          }
          storage.total_minerals(0f.coerceAtLeast(storage.total_minerals - mineralsUsedThisTurn))
          br.delta_minerals_per_hour(-mineralsUsedThisTurn * STEP_TIME / Time.HOUR)
          mineralsDeltaPerHour -= mineralsUsedThisTurn * STEP_TIME / Time.HOUR
          log("     Used: [minerals=%.4f]", mineralsUsedThisTurn)

          // what is the current amount of time we have now as a percentage of the total build
          // time?
          if (progressThisTurn + br.progress >= 1.0f) {
            // OK, we've finished! Work out how far into the step we completed.
            val unusedProgress = progressThisTurn + br.progress - 1.0f
            val fractionProgress = (progressThisTurn - unusedProgress) / progressThisTurn
            var endTime = now - STEP_TIME
            if (br.start_time > endTime) {
              endTime = br.start_time
            }
            endTime += (STEP_TIME * fractionProgress).toLong()
            log("     FINISHED! progress-this-turn: %.2f fraction-progress = %.2f, end-time=%d",
                progressThisTurn, fractionProgress, endTime)
            br.progress(1.0f)
            br.progress_per_step(progressThisTurn)
            br.end_time(endTime)
            completeBuildRequests.add(br.build())
            continue
          }

          // work hasn't finished yet, so lets estimate how long it will take now
          val remainingWorkersRequired =
              buildCost.population * (1.0f - br.progress - progressThisTurn) * br.count
          val remainingMineralsRequired =
              buildCost.minerals * (1.0f - br.progress - progressThisTurn) * br.count
          val timeForMineralsSteps = remainingMineralsRequired / mineralsPerBuildRequest
          val timeForPopulationSteps = remainingWorkersRequired / workersPerBuildRequest
          val timeForMineralsHours = timeForMineralsSteps * STEP_TIME / Time.HOUR
          val timeForPopulationHours = timeForPopulationSteps * STEP_TIME / Time.HOUR
          log("     Remaining: [minerals=%.2f hrs %.2f steps] [population=%.2f hrs %.2f steps]",
              timeForMineralsHours,
              timeForMineralsSteps,
              timeForPopulationHours,
              timeForPopulationSteps)
          val endTime =
              now + (timeForMineralsHours.coerceAtLeast(timeForPopulationHours) * Time.HOUR)
                  .roundToInt()
          br.end_time(endTime)
          log("     Finish time: %d (now=%d)", endTime, now)
          br.progress(br.progress + progressThisTurn)
          br.progress_per_step(progressThisTurn)

          // Calculate the efficiency of the minerals vs. population
          val sumTimeInHours = timeForMineralsHours + timeForPopulationHours
          val mineralsEfficiency = 1 - timeForMineralsHours / sumTimeInHours
          val populationEfficiency = 1 - timeForPopulationHours / sumTimeInHours
          br.minerals_efficiency(mineralsEfficiency)
          br.population_efficiency(populationEfficiency)
          log("     Efficiency: [minerals=%.3f] [population=%.3f]",
              mineralsEfficiency, populationEfficiency)
          completeBuildRequests.add(br.build())
        }
        star.planets[i] = planet.colony(
            colony.build_requests(completeBuildRequests).build()).build()
      }
    }

    // Finally, update the population. The first thing we need to do is evenly distribute goods
    // between all of the colonies.
    val totalGoodsPerHour = 10.0f.coerceAtMost(totalPopulation / 10.0f)
    val totalGoodsRequired = totalGoodsPerHour * dt
    goodsDeltaPerHour -= totalGoodsPerHour

    // If we have more than total_goods_required stored, then we're cool. Otherwise, our population
    // suffers...
    var goodsEfficiency = 1.0f
    if (totalGoodsRequired > storage.total_goods && totalGoodsRequired > 0) {
      goodsEfficiency = storage.total_goods / totalGoodsRequired
    }
    log("--- Updating Population [goods required=%.2f] [goods available=%.2f] [efficiency=%.2f]",
        totalGoodsRequired, storage.total_goods, goodsEfficiency)

    // subtract all the goods we'll need
    storage.total_goods(storage.total_goods - totalGoodsRequired)
    if (storage.total_goods <= 0.0f) {
      // We've run out of goods! That's bad...
      storage.total_goods(0.0f)
      if (storage.goods_zero_time == null || storage.goods_zero_time > now) {
        log("    GOODS HAVE HIT ZERO")
        storage.goods_zero_time(now)
      }
    }

    // now loop through the colonies and update the population/goods counter
    for (i in star.planets.indices) {
      val planet = star.planets[i]
      if (planet.colony == null) {
        continue
      }
      val colony = planet.colony.newBuilder()
      if (!equalEmpire(colony.empire_id, empireId)) {
        continue
      }
      var populationIncrease: Float
      if (goodsEfficiency >= 1.0f) {
        populationIncrease = max(colony.population, 10.0f) * 0.1f
      } else {
        populationIncrease = max(colony.population, 10.0f)
        populationIncrease *= 0.9f
        populationIncrease *= 0.25f * (goodsEfficiency - 1.0f)
      }
      colony.delta_population(populationIncrease)
      val populationIncreaseThisTurn = populationIncrease * dt
      if (colony.cooldown_end_time != null && colony.cooldown_end_time < now) {
        log("    Colony is no longer in cooldown period.")
        colony.cooldown_end_time = null
      }
      val maxPopulation = getMaxPopulation(planet)
      var newPopulation = colony.population + populationIncreaseThisTurn
      if (newPopulation < 1.0f) {
        newPopulation = 0.0f
      } else if (newPopulation > maxPopulation) {
        newPopulation = maxPopulation.toFloat()
      }
      if (newPopulation < 100.0f && colony.cooldown_end_time != null) {
        newPopulation = 100.0f
      }
      log("    Colony[%d]: [delta=%.2f] [new=%.2f]", i, populationIncrease, newPopulation)
      colony.population(newPopulation)
      star.planets[i] = planet.newBuilder().colony(colony.build()).build()
    }
    if (storage.total_goods > storage.max_goods) {
      storage.total_goods = storage.max_goods
    }
    if (storage.total_minerals > storage.max_minerals) {
      storage.total_minerals = storage.max_minerals
    }
    if (storage.total_energy > storage.max_energy) {
      storage.total_energy = storage.max_energy
    }
    storage.goods_delta_per_hour(goodsDeltaPerHour)
    storage.minerals_delta_per_hour(mineralsDeltaPerHour)
    storage.energy_delta_per_hour(energyDeltaPerHour)
    star.empire_stores[storageIndex] = storage.build()
    log(String.format(Locale.ENGLISH,
        "-- Store: goods=%.2f (%.2f/hr) minerals=%.2f (%.2f/hr) energy=%.2f (%.2f/h)",
        storage.total_goods, storage.goods_delta_per_hour,
        storage.total_minerals, storage.minerals_delta_per_hour,
        storage.total_energy, storage.energy_delta_per_hour))
  }

  /**
   * Simulate combat on the star.
   *
   * <p>Combat runs in rounds, but rounds do not take any "time". Each round every fleet that is
   * attacking find a target and attacks it. The number of fleets destroyed by the attack is
   * simply the attacking fleet's attack stat multiplied by the number of ships, divided by the
   * defending fleet's defense stat.
   *
   * <p>If a fleet is destroyed by the attack, the remaining attack points are then used to target
   * another fleet in the same round until there's no more attack points left. This is so that you
   * do not get an advantage by splitting up all your fleets.
   */
  private fun simulateCombat(
      star: Star.Builder,
      now: Long,
      sitReports: MutableMap<Long, SituationReport.Builder>?) {
    // if there's no fleets in ATTACKING mode, then there's nothing to do
    if (!anyFleetsAttacking(star)) {
      return
    }

    // Create a new combat report, and save the current fleets to it.
    val combatReportBuilder = CombatReport.Builder()
        .time(now)
        .fleets_before(ArrayList(star.fleets))
    log("Begin combat for '%s'", star.name)
    var roundNumber = 1
    do {
      log(" - Combat round %d", roundNumber)
      simulateCombatRound(star, now)
      roundNumber++
    } while (anyFleetsAttacking(star))

    // Add the combat report to the star, and remove any if there's more than 10 in the history.
    combatReportBuilder.fleets_after(ArrayList(star.fleets))
    star.combat_reports.add(0, combatReportBuilder.build())
    while (star.combat_reports.size > 10) {
      star.combat_reports.removeAt(star.combat_reports.size - 1)
    }

    if (sitReports != null) {
      // Make sure we have all the empires set up in the situation report to begin with.
      for (fleet in combatReportBuilder.fleets_before) {
        if (sitReports[fleet.empire_id] == null) {
          sitReports[fleet.empire_id] = SituationReport.Builder()
              .empire_id(fleet.empire_id)
              .star_id(star.id)
              .report_time(System.currentTimeMillis())
        }
      }

      // Now calculate the losses
      val fleetsLost = HashMap<Long, EnumMap<Design.DesignType, Float>>()
      for (fleetBefore in combatReportBuilder.fleets_before) {
        var wasDestroyed = true
        var numDestroyed = 0.0f
        for (fleetAfter in combatReportBuilder.fleets_after) {
          if (fleetAfter.id == fleetBefore.id) {
            wasDestroyed = false
            numDestroyed = fleetAfter.num_ships - fleetBefore.num_ships
          }
        }
        if (wasDestroyed) {
          numDestroyed = fleetBefore.num_ships
        }

        var thisFleetLost = fleetsLost[fleetBefore.empire_id]
        if (thisFleetLost == null) {
          thisFleetLost = EnumMap<Design.DesignType, Float>(Design.DesignType::class.java)
          fleetsLost[fleetBefore.empire_id] = thisFleetLost
        }
        thisFleetLost[fleetBefore.design_type] =
            (thisFleetLost[fleetBefore.design_type] ?: 0f) + numDestroyed
      }

      // Finally, populate all the sit reports. Each sit-report gets one copy of the losses.
      for (entry in sitReports) {
        val empireId = entry.key
        val sitReport = entry.value

        for (combatEntry in fleetsLost) {
          val isEnemy = empireId != combatEntry.key

          for (lossEntry in combatEntry.value) {
            val fleetRecord = SituationReport.FleetRecord.Builder()
                .design_type(lossEntry.key)
                .num_ships(lossEntry.value)
                .build()
            if (isEnemy) {
              sitReport.fleet_victorious_record.add(fleetRecord)
            } else {
              sitReport.fleet_destroyed_record.add(fleetRecord)
            }
          }
        }
      }
    }
  }

  private fun anyFleetsAttacking(star: Star.Builder): Boolean {
    var numAttacking = 0
    for (fleet in star.fleets) {
      if (fleet.state != Fleet.FLEET_STATE.ATTACKING
          || fleet.is_destroyed != null && fleet.is_destroyed) {
        continue
      }
      numAttacking++
    }
    return numAttacking > 0
  }

  /** Simulate a single round of combat on the given star.  */
  private fun simulateCombatRound(star: Star.Builder, now: Long) {
    val damageCounter: MutableMap<Long, Double> = HashMap()
    for (i in star.fleets.indices) {
      val fleet = star.fleets[i]
      if (fleet.state == Fleet.FLEET_STATE.ATTACKING) {
        // Work out how much attacking power this fleet has.
        val design = getDesign(fleet.design_type)
        var attack = fleet.num_ships * design.base_attack.toDouble()
        log("   - Fleet=[%d %s] numShips=%.2f", fleet.id, design.display_name, fleet.num_ships)
        while (attack > 0.0) {
          val target = findTarget(star, fleet, damageCounter)
          if (target == null) {
            log("      No target.")
            // No target was found, there's nothing left to attack.
            star.fleets[i] = star.fleets[i].newBuilder()
                .state(Fleet.FLEET_STATE.IDLE)
                .state_start_time(now)
                .build()
            break
          } else {
            // Got a target, work out how much damage this fleet has already taken.
            var numShips = target.num_ships.toDouble()
            var previousDamage = 0.0
            if (damageCounter.containsKey(target.id)) {
              previousDamage = damageCounter[target.id]!!
              numShips -= previousDamage
            }
            log("      Target=[%d] numShips=%.4f * %.2f <-- attack=%.4f",
                target.id, numShips, design.base_defence, attack)
            numShips *= design.base_defence.toDouble()
            if (numShips >= attack) {
              // If there's more ships than we have attack capability, just apply the damage.
              damageCounter[target.id] = previousDamage + attack / design.base_defence
              attack -= numShips
            } else {
              // If we have more attack capability than they have ships, they're dead.
              damageCounter[target.id] = target.num_ships.toDouble()
              attack = 0.0
            }
          }
        }
      }
    }
    if (damageCounter.isNotEmpty()) {
      log("   -- Applying damage...")
      // Now that everyone has attacked, apply the damage.
      for (i in star.fleets.indices) {
        val fleet = star.fleets[i]
        val damage = damageCounter[fleet.id] ?: continue
        if (fleet.num_ships - damage <= EPSILON) {
          log("      Fleet=%d destroyed (num_ships=%.4f <= damage=%.4f).",
              fleet.id, fleet.num_ships, damage)
          star.fleets[i] = fleet.newBuilder()
              .is_destroyed(true)
              .num_ships(0.0f)
              .build()
        } else {
          // They'll be attacking next round (unless their stance is passive).
          var state: Fleet.FLEET_STATE? = Fleet.FLEET_STATE.ATTACKING
          if (fleet.stance == Fleet.FLEET_STANCE.PASSIVE) {
            state = fleet.state
          }
          log("      Fleet=%d numShips=%.8f damage=%.8f state=%s.",
              fleet.id, fleet.num_ships, damage, state!!)
          star.fleets[i] = fleet.newBuilder()
              .num_ships(fleet.num_ships - damage.toFloat())
              .state(state)
              .build()
        }
      }
    } else {
      log("   -- No damage to apply.")
    }
  }

  /**
   * Searches for an enemy fleet with the lowest priority.
   *
   * @param star The star we're searching on.
   * @param fleet The fleet we're searching for a target for.
   * @param damageCounter A mapping of fleet IDs to the damage they've taken so far this round (so
   * that we don't target them if they're already destroyed).
   */
  private fun findTarget(star: Star.Builder, fleet: Fleet, damageCounter: Map<Long, Double>): Fleet? {
    var foundPriority = 9999
    var target: Fleet? = null
    for (potentialTarget in star.fleets) {
      // If it's moving we can't attack it.
      if (potentialTarget.state == Fleet.FLEET_STATE.MOVING) {
        continue
      }

      // If it's destroyed, it's not a good target.
      if (potentialTarget.is_destroyed != null && potentialTarget.is_destroyed) {
        continue
      }

      // If it's friendly, we can't attack it,
      if (isFriendly(fleet, potentialTarget)) {
        continue
      }

      // If it's already destroyed, we can't attack it.
      val damage = damageCounter[potentialTarget.id]
      if (damage != null && damage >= potentialTarget.num_ships) {
        continue
      }

      // If its priority is higher than the one we've already found, then don't bother.
      val design = getDesign(potentialTarget.design_type)
      if (design.combat_priority > foundPriority) {
        continue
      }
      foundPriority = design.combat_priority
      target = potentialTarget
    }
    return target
  }

  /**
   * Simulates energy transports. We only do this once, when we finish simulating before we go
   * into the prediction phase. If there's any energy transports on the star, transfer their energy
   * to non-energy transport fleets of the same empire.
   */
  private fun simulateEnergyTransports(star: Star.Builder, now: Long) {
    log("Refueling")

    // Find all the energy transport fleets. If there's none we'll try to avoid allocating any extra
    // memory.
    var energyTransportIndices: MutableList<Int?>? = null
    for (i in star.fleets.indices) {
      val fleet = star.fleets[i]
      if (hasEffect(fleet, Design.EffectType.ENERGY_TRANSPORT)) {
        if (energyTransportIndices == null) {
          energyTransportIndices = ArrayList()
        }
        energyTransportIndices.add(i)
      }
    }

    // No energy transports.
    if (energyTransportIndices != null) {
      log(" - %d energy transports available, refueling from there first",
          energyTransportIndices.size)
      for (energyTransportIndex in energyTransportIndices) {
        var energyTransportFleet = star.fleets[energyTransportIndex!!]
        for (i in star.fleets.indices) {
          val fleet = star.fleets[i]
          if (hasEffect(fleet, Design.EffectType.ENERGY_TRANSPORT)) {
            continue
          }
          if (!isFriendly(fleet, energyTransportFleet)) {
            continue
          }
          val design = getDesign(fleet.design_type)
          val requiredEnergy = design.fuel_size * fleet.num_ships - fleet.fuel_amount
          if (requiredEnergy <= 0) {
            continue
          }
          val availableEnergy = energyTransportFleet.fuel_amount
          if (availableEnergy > requiredEnergy) {
            log(" - filling fleet %d (%.2f required) from energy transport %d: %.2f fuel left in "
                + "transport",
                fleet.id, requiredEnergy, energyTransportFleet.id,
                energyTransportFleet.fuel_amount - requiredEnergy)
            energyTransportFleet = energyTransportFleet.newBuilder()
                .fuel_amount(availableEnergy - requiredEnergy)
                .build()
            star.fleets[energyTransportIndex] = energyTransportFleet
            star.fleets[i] = fleet.newBuilder().fuel_amount(fleet.fuel_amount + requiredEnergy).build()
          } else {
            log(" - filling fleet %d (with %.2f fuel, %.2f required) from now-empty energy "
                + "transport #%d",
                fleet.id, availableEnergy, requiredEnergy, energyTransportFleet.id)
            star.fleets[energyTransportIndex] = energyTransportFleet.newBuilder().fuel_amount(0f).build()
            star.fleets[i] = fleet.newBuilder().fuel_amount(fleet.fuel_amount + availableEnergy).build()

            // No point continuing, we're empty.
            break
          }
        }
      }
    }

    // Next, try to refuel everything from our storage(s).
    for (i in star.fleets.indices) {
      val fleet = star.fleets[i].newBuilder()
      val design = getDesign(fleet.design_type)
      val neededFuelTotal = design.fuel_size.toFloat() * fleet.num_ships
      if (fleet.fuel_amount < neededFuelTotal) {
        val storageIndex = getStoreIndex(star, fleet.empire_id)
        if (storageIndex < 0) {
          // No storages for this empire, nothing to do.
          continue
        }
        val storage = star.empire_stores[storageIndex].newBuilder()
        val neededFuelRemaining = neededFuelTotal - fleet.fuel_amount
        val actual = neededFuelRemaining.coerceAtMost(storage.total_energy)
        star.fleets[i] = fleet.fuel_amount(fleet.fuel_amount + actual).build()
        star.empire_stores[storageIndex] = storage.total_energy(storage.total_energy - actual).build()
        log("--- Fleet %d [%s x %.0f] re-fueling: %.2f",
            fleet.id, design.display_name, fleet.num_ships, actual)
      }
      star.fleets[i] = fleet.build()
    }
  }

  private fun log(format: String, vararg args: Any) {
    logHandler?.log(String.format(Locale.US, format, *args))
  }

  /**
   * This interface is used to help debug the simulation code. Implement it to receive a bunch
   * of debug log messages during the simulation process.
   */
  interface LogHandler {
    fun setStarName(starName: String?)
    fun log(message: String)
  }

  private class BasicLogHandler : LogHandler {
    private var starName: String? = null
    override fun setStarName(starName: String?) {
      this.starName = starName
    }

    override fun log(message: String) {
      log.info("$starName - $message")
    }

    companion object {
      private val log = Log("Simulation")
    }
  }

  companion object {
    private const val sDebug = false

    /**
     * A small floating-point value, if you have fewer ships than this, then you basically have none.
     */
    private const val EPSILON = 0.1

    /** Step time is 10 minutes.  */
    const val STEP_TIME = 10 * Time.MINUTE
    private fun equalEmpire(one: Long?, two: Long?): Boolean {
      if (one == null && two == null) {
        return true
      }
      return if (one == null || two == null) {
        false
      } else one == two
    }
  }
}
