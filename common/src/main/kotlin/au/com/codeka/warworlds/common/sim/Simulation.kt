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
import com.google.common.collect.Lists
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** This class is used to simulate a [Star].  */
class Simulation constructor(
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
   * @param star The [Star] to simulate. We return a new star with the simulated values.
   * @param sitReports A map of situation reports that we will population all the situation reports
   * generated by this simulation (it could have some pre-populated if you want us to add to them
   * instead of creating new ones).
   * @return A new [Star] that has been simulated.
   */
  fun simulate(star: Star, sitReports: MutableMap<Long, SituationReport>? = null): Star {
    val mutableStar = MutableStar.from(star)
    simulate(mutableStar, sitReports)
    return mutableStar.build()
  }

  /**
   * Simulate the given [MutableStar], and make sure it's "current".
   *
   * @param star The [MutableStar] to simulate. We modify it in-place.
   * @param sitReports A map of situation reports that we will population all the situation reports
   * generated by this simulation (it could have some pre-populated if you want us to add to them
   * instead of creating new ones).
   */
  fun simulate(
      star: MutableStar,
      sitReports: MutableMap<Long, SituationReport>? = null) {
    logHandler?.setStarName(star.name)
    log("Begin simulation for '${star.name}' @ $timeOverride")

    // figure out the start time, which is the oldest last_simulation time
    val startTime = getSimulateStartTime(star)
    val endTime = trimTimeToStep(timeOverride)
    val empireIds = HashSet<Long>()
    for (planet in star.planets) {
      val colony = planet.colony
      if (colony != null) {
        empireIds.add(colony.empireId)
      }
    }

    // We'll simulate in "prediction mode" for an extra bit of time so that we can get a more
    // accurate estimate of the end time for builds. We won't *record* the population growth and
    // such, just the end time of builds. We'll also record the time that the population drops below
    // a certain threshold so that we can warn the player.
    val predictionTime = endTime + Time.DAY
    var predictionStar: MutableStar? = null
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
        simulateEnergyTransports(star)

        // We always predict at least one more step, so that we can put the deltas from the next
        // step in (since they'll take into account things like focus changes, new builds, etc that
        // the user has applied in THIS step).
        predictionStar = MutableStar.from(star.build())
        log("Begin prediction")
        simulateStepForAllEmpires(now, predictionStar, empireIds)
        copyDeltas(now, star, predictionStar)
      } else if (predict && now <= predictionTime) {
        simulateStepForAllEmpires(now, predictionStar, empireIds)
      } else {
        break
      }
      now += STEP_TIME
    }

    // Copy the end times for builds from the prediction star.
    for (planet in star.planets) {
      val predictionPlanet = predictionStar!!.planets[planet.index]
      if (planet.colony == null || predictionPlanet.colony == null) {
        continue
      }
      for (br in planet.colony!!.buildRequests) {
        for (predictionBuildRequest in predictionPlanet.colony!!.buildRequests) {
          if (predictionBuildRequest.id == br.id) {
            br.endTime = predictionBuildRequest.endTime
          }
        }
      }
    }

    star.lastSimulation = timeOverride
  }

  /**
   * After simulating the first step in the prediction star, copy the mineral, goods and energy
   * deltas across to the main star. Also copy the build request efficiencies, since they're also
   * the ones we'll care about (i.e. the efficient for the *next* step).
   */
  private fun copyDeltas(now: Long, star: MutableStar, predictionStar: MutableStar) {
    for (storage in star.empireStores) {
      val predictionStore = getStore(predictionStar, storage.empireId)
          // If the empire has been wiped out or something, just ignore.
          ?: continue

      var mineralsDeltaPerHour = predictionStore.mineralsDeltaPerHour
      for (planet in star.planets) {
        val colony = planet.colony
        if (colony != null) {
          for (br in colony.buildRequests) {
            if (br.endTime < now) {
              // If this build request has finished by 'now' then it's delta shouldn't be counted
              mineralsDeltaPerHour += br.mineralsDeltaPerHour
            }
          }
        }
      }

      storage.goodsDeltaPerHour = predictionStore.goodsDeltaPerHour
      storage.mineralsDeltaPerHour = mineralsDeltaPerHour
      storage.energyDeltaPerHour = predictionStore.energyDeltaPerHour
    }

    for (planet in star.planets) {
      val colony = planet.colony ?: continue
      for (br in colony.buildRequests) {
        val predictionBuildRequest =
            ColonyHelper.findBuildRequest(predictionStar, br.id) ?: continue
        br.mineralsEfficiency = predictionBuildRequest.mineralsEfficiency
        br.populationEfficiency = predictionBuildRequest.populationEfficiency
        br.progressPerStep = predictionBuildRequest.progressPerStep
        br.mineralsDeltaPerHour = predictionBuildRequest.mineralsDeltaPerHour
      }
    }
  }

  /**
   * Gets the time we should start simulating this star for.
   *
   * Most of the time, this will be the time the star was last simulated, but in the case of native-
   * only stars, we'll just simulate the last 24 hours.
   */
  private fun getSimulateStartTime(star: MutableStar): Long {
    var lastSimulation = star.lastSimulation

    // if there's only native colonies, don't bother simulating from more than
    // 24 hours ago. The native colonies will generally be in a steady state
    val oneDayAgo = timeOverride - Time.DAY
    if (lastSimulation != null && lastSimulation < oneDayAgo) {
      log("Last simulation more than on day ago, checking whether there are any non-native colonies.")
      var onlyNativeColonies = true
      for (planet in star.planets) {
        if (planet.colony?.empireId != null) {
          onlyNativeColonies = false
          break
        }
      }
      for (fleet in star.fleets) {
        if (fleet.empireId != 0L) {
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

  private fun simulateStepForAllEmpires(now: Long, star: MutableStar, empireIds: Set<Long>) {
    log("- Step [now=%d]", now)
    for (empireId in empireIds) {
      log(String.format("-- Empire [%d]", empireId))
      simulateStep(now, star, empireId)
    }

    // Remove stores for any empires that don't exist anymore.
    var i = 0
    while (i < star.empireStores.size) {
      if (!empireIds.contains(star.empireStores[i].empireId)) {
        star.empireStores.removeAt(i)
        i--
      }
      i++
    }
  }

  private fun simulateStep(now: Long, star: MutableStar, empireId: Long) {
    var totalPopulation = 0.0f
    val storageIndex = getStoreIndex(star, empireId)
    if (storageIndex < 0) {
      log("No storage found for this empire!")
      return
    }
    val storage = star.empireStores[storageIndex]
    storage.maxGoods = 200.0f
    storage.maxEnergy = 5000.0f
    storage.maxMinerals = 5000.0f
    val dt = toHours(STEP_TIME)
    var goodsDeltaPerHour = 0.0f
    var mineralsDeltaPerHour = 0.0f
    var energyDeltaPerHour = 0.0f
    for (planet in star.planets) {
      val colony = planet.colony ?: continue
      if (colony.empireId != empireId) {
        continue
      }

      // Apply storage bonuses this round (note: this could change from step to step, if a building
      // finishes being built, for example).
      for (buildingIndex in colony.buildings.indices) {
        val building = colony.buildings[buildingIndex]
        val design = getDesign(building.designType)

        // Validate the building's level.
        if (building.level - 1 > design.upgrades.size) {
          building.level = design.upgrades.size + 1
        }

        var effects = design.effect
        if (building.level > 1) {
          // Level will be 2 for the first upgrade, 3 for the second and so on.
          effects = design.upgrades[building.level - 2].effects
        }
        for (effect in effects) {
          if (effect.type == Design.EffectType.STORAGE) {
            storage.maxGoods += effect.goods!!.toFloat()
            storage.maxMinerals += effect.minerals!!.toFloat()
            storage.maxEnergy += effect.energy!!.toFloat()
          }
        }
      }

      // Some sanity checks.
      if (isInvalid(storage.totalEnergy)) {
        storage.totalEnergy = 0.0f
      }
      if (isInvalid(storage.totalGoods)) {
        storage.totalGoods = 0.0f
      }
      if (isInvalid(storage.totalMinerals)) {
        storage.totalMinerals = 0.0f
      }
      if (isInvalid(colony.focus.construction) ||
          isInvalid(colony.focus.farming) ||
          isInvalid(colony.focus.mining) ||
          isInvalid(colony.focus.energy)) {
        colony.focus.construction = 0.25f
        colony.focus.farming = 0.25f
        colony.focus.mining = 0.25f
        colony.focus.energy = 0.25f
      }
      log("--- Colony [planetIndex=%d] [population=%.2f / %d]",
          planet.index, colony.population, getMaxPopulation(planet.proto))

      // Calculate the output from farming this turn and add it to the star global
      val goods =
          colony.population * colony.focus.farming *
              (ColonyHelper.getFarmingCongeniality(planet.proto) / 100.0f)
      colony.deltaGoods = goods
      storage.totalGoods = 0f.coerceAtLeast(storage.totalGoods + goods * dt)
      goodsDeltaPerHour += goods
      log("    Goods: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.totalGoods, goods, goods * dt)

      // calculate the output from mining this turn and add it to the star global
      val mineralsPerHour =
          colony.population * colony.focus.mining *
              (ColonyHelper.getMiningCongeniality(planet.proto) / 10.0f)
      colony.deltaMinerals = mineralsPerHour
      storage.totalMinerals = 0f.coerceAtLeast(storage.totalMinerals + mineralsPerHour * dt)
      mineralsDeltaPerHour += mineralsPerHour
      log("    Minerals: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.totalMinerals, mineralsPerHour, mineralsPerHour * dt)

      // calculate the output from energy this turn and add it to the star global
      val energy =
          colony.population * colony.focus.energy *
              (ColonyHelper.getEnergyCongeniality(planet.proto) / 10.0f)
      colony.deltaEnergy = energy
      storage.totalEnergy = 0f.coerceAtLeast(storage.totalEnergy + energy * dt)
      energyDeltaPerHour += energy
      log("    Energy: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.totalEnergy, energy, energy * dt)
      totalPopulation += colony.population
    }

    // A second loop though the colonies, once the goods/minerals/energy has been calculated.
    for (planet in star.planets) {
      val colony = planet.colony ?: continue
      if (colony.empireId != empireId) {
        continue
      }

      // not all build requests will be processed this turn. We divide up the population
      // based on the number of ACTUAL build requests they'll be working on this turn
      var numValidBuildRequests = 0
      for (br in colony.buildRequests) {
        if (br.startTime >= now) {
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
        val mineralsPerBuildRequest = storage.totalMinerals / numValidBuildRequests
        log("--- Building [buildRequests=%d] [planetIndex=%d] [totalWorker=%.2f] [totalMinerals=%.2f]",
            numValidBuildRequests, planet.index, totalWorkers, storage.totalMinerals)

        // OK, we can spare at least ONE population
        if (workersPerBuildRequest < 1.0f) {
          workersPerBuildRequest = 1.0f
        }

        for (br in colony.buildRequests) {
          // Sanity check.
          if (isInvalid(br.progress)) {
            br.progress = 0f
          }
          val design = getDesign(br.designType)
          val startTime = br.startTime
          if (startTime > now || br.progress >= 1.0f) {
            continue
          }

          // the build cost is defined by the original design, or possibly by the upgrade if that
          // is what it is.
          var buildCost = design.build_cost
          if (br.buildingId != null) {
            for (building in colony.buildings) {
              if (building.id == br.buildingId) {
                // The building is at it's "old" level, so the cost is the cost of the *next* level.
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
          log("---- Building [id=%d] [design=%s %s] [count=%d] cost [workers=%.2f]" +
              " [minerals=%.2f] [start-time=%d]",  br.id, design.design_kind, design.type, br.count,
              buildCost.population, buildCost.minerals, br.startTime)

          // The total amount of time to build something is based on the number of workers it
          // requires, if you have the right number of workers and the right amount of minerals,
          // you can finish the build in one turn. However, if you only have a fraction of them
          // available, then that fraction of progress will be made.
          val totalWorkersRequired = buildCost.population * br.count
          val totalMineralsRequired = buildCost.minerals * br.count
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
          if (br.startTime > now) {
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
          storage.totalMinerals = 0f.coerceAtLeast(storage.totalMinerals - mineralsUsedThisTurn)
          br.mineralsDeltaPerHour = -mineralsUsedThisTurn * STEP_TIME / Time.HOUR
          mineralsDeltaPerHour -= mineralsUsedThisTurn * STEP_TIME / Time.HOUR
          log("     Used: [minerals=%.4f]", mineralsUsedThisTurn)

          // what is the current amount of time we have now as a percentage of the total build
          // time?
          if (progressThisTurn + br.progress >= 1.0f) {
            // OK, we've finished! Work out how far into the step we completed.
            val unusedProgress = progressThisTurn + br.progress - 1.0f
            val fractionProgress = (progressThisTurn - unusedProgress) / progressThisTurn
            var endTime = now - STEP_TIME
            if (br.startTime > endTime) {
              endTime = br.startTime
            }
            endTime += (STEP_TIME * fractionProgress).toLong()
            log("     FINISHED! progress-this-turn: %.2f fraction-progress = %.2f, end-time=%d",
                progressThisTurn, fractionProgress, endTime)
            br.progress = 1.0f
            br.progressPerStep = progressThisTurn
            br.endTime = endTime
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
          br.endTime = endTime.roundToLong()
          log("     Finish time: %f (now=%d)", endTime, now)
          br.progress = br.progress + progressThisTurn
          br.progressPerStep = progressThisTurn

          // Calculate the efficiency of the minerals vs. population
          val sumTimeInHours = timeForMineralsHours + timeForPopulationHours
          val mineralsEfficiency = 1 - timeForMineralsHours / sumTimeInHours
          val populationEfficiency = 1 - timeForPopulationHours / sumTimeInHours
          br.mineralsEfficiency = mineralsEfficiency
          br.populationEfficiency = populationEfficiency
          log("     Efficiency: [minerals=%.3f] [population=%.3f]",
              mineralsEfficiency, populationEfficiency)
        }
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
    if (totalGoodsRequired > storage.totalGoods && totalGoodsRequired > 0) {
      goodsEfficiency = storage.totalGoods / totalGoodsRequired
    }
    log("--- Updating population [goods required=%.2f] [goods available=%.2f] [efficiency=%.2f]",
        totalGoodsRequired, storage.totalGoods, goodsEfficiency)

    // subtract all the goods we'll need
    storage.totalGoods -= totalGoodsRequired
    if (storage.totalGoods <= 0.0f) {
      // We've run out of goods! That's bad...
      storage.totalGoods = 0.0f
      if (storage.goodsZeroTime ?: Long.MAX_VALUE > now) {
        log("    GOODS HAVE HIT ZERO")
        storage.goodsZeroTime = now
      }
    }

    // now loop through the colonies and update the population/goods counter
    for (planet in star.planets) {
      val colony = planet.colony ?: continue
      if (colony.empireId != empireId) {
        continue
      }

      var populationIncrease: Float
      if (goodsEfficiency >= 1.0f) {
        // Increase population exponentially: the less population there is, the more people will
        // try to make more babies...
        val difference = planet.populationCongeniality - colony.population
        populationIncrease = max(difference, 100.0f) * 0.25f
      } else {
        populationIncrease = max(colony.population, 100.0f)
        populationIncrease *= 0.9f
        populationIncrease *= 0.25f * (goodsEfficiency - 1.0f)
      }
      colony.deltaPopulation = populationIncrease
      val populationIncreaseThisTurn = populationIncrease * dt
      if (colony.cooldownEndTime ?: Long.MAX_VALUE < now) {
        log("    Colony is no longer in cooldown period.")
        colony.cooldownEndTime = null
      }
      val maxPopulation = getMaxPopulation(planet.proto)
      var newPopulation = colony.population + populationIncreaseThisTurn
      if (newPopulation < 1.0f) {
        newPopulation = 0.0f
      } else if (newPopulation > maxPopulation) {
        newPopulation = maxPopulation.toFloat()
      }
      if (newPopulation < 100.0f && colony.cooldownEndTime != null) {
        newPopulation = 100.0f
      }
      log("    Colony[%d]: [delta=%.2f] [new=%.2f] [max=%d]",
          planet.index, populationIncrease, newPopulation, maxPopulation)
      colony.population = newPopulation
    }
    storage.totalGoods = storage.totalGoods.coerceAtMost(storage.maxGoods)
    storage.totalMinerals = storage.totalMinerals.coerceAtMost(storage.maxMinerals)
    storage.totalEnergy = storage.totalEnergy.coerceAtMost(storage.maxEnergy)
    storage.goodsDeltaPerHour = goodsDeltaPerHour
    storage.mineralsDeltaPerHour = mineralsDeltaPerHour
    storage.energyDeltaPerHour = energyDeltaPerHour
    log(String.format(Locale.ENGLISH,
        "-- Store: goods=%.2f (%.2f/hr) minerals=%.2f (%.2f/hr) energy=%.2f (%.2f/h)",
        storage.totalGoods, storage.goodsDeltaPerHour,
        storage.totalMinerals, storage.mineralsDeltaPerHour,
        storage.totalEnergy, storage.energyDeltaPerHour))
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
      star: MutableStar,
      now: Long,
      sitReports: MutableMap<Long, SituationReport>?) {
    // if there's no fleets in ATTACKING mode, then there's nothing to do
    if (!anyFleetsAttacking(star)) {
      return
    }

    // Create a new combat report, and save the current fleets to it.
    val combatReport = MutableCombatReport(CombatReport(time = now))
    combatReport.fleetsBefore = star.fleets.map { it.build() }

    log("Begin combat for '%s'", star.name)
    var roundNumber = 1
    do {
      log(" - Combat round %d", roundNumber)
      simulateCombatRound(star, now)
      roundNumber++
    } while (anyFleetsAttacking(star))

    // Add the combat report to the star, and remove any if there's more than 10 in the history.
    combatReport.fleetsAfter = star.fleets.map { it.build() }
    star.combatReports.add(0, combatReport)
    while (star.combatReports.size > 10) {
      star.combatReports.removeAt(star.combatReports.size - 1)
    }

    if (sitReports != null) {
      populateCombatSitReports(
          star, combatReport.fleetsBefore, combatReport.fleetsAfter, sitReports)
    }
  }

  private fun anyFleetsAttacking(star: MutableStar): Boolean {
    var numAttacking = 0
    for (fleet in star.fleets) {
      if (fleet.state != Fleet.FLEET_STATE.ATTACKING || fleet.isDestroyed) {
        continue
      }
      numAttacking++
    }
    return numAttacking > 0
  }

  /** Simulate a single round of combat on the given star.  */
  private fun simulateCombatRound(star: MutableStar, now: Long) {
    val damageCounter: MutableMap<Long, Double> = HashMap()
    for (fleet in star.fleets) {
      if (fleet.state == Fleet.FLEET_STATE.ATTACKING) {
        // Work out how much attacking power this fleet has.
        val design = getDesign(fleet.designType)
        var attack = fleet.numShips * design.base_attack!!.toDouble()
        log("   - Fleet=[%d %s] numShips=%.2f", fleet.id, design.display_name, fleet.numShips)
        while (attack > 0.0) {
          val target = findTarget(star, fleet, damageCounter)
          if (target == null) {
            log("      No target.")
            // No target was found, there's nothing left to attack.
            fleet.state = Fleet.FLEET_STATE.IDLE
            fleet.stateStartTime = now
            break
          } else {
            // Got a target, work out how much damage this fleet has already taken.
            val targetDesign = getDesign(target.designType)
            var numShips = target.numShips.toDouble()
            var previousDamage = 0.0
            if (damageCounter.containsKey(target.id)) {
              previousDamage = damageCounter[target.id]!!
              numShips -= previousDamage
            }
            log("      Target=[%d %s] numShips=%.4f * %.2f <-- attack=%.4f",
                target.id, targetDesign.display_name, numShips, design.base_defence!!, attack)
            numShips *= targetDesign.base_defence!!.toDouble()
            if (numShips >= attack) {
              // If there's more ships than we have attack capability, just apply the damage.
              damageCounter[target.id] = previousDamage + attack / targetDesign.base_defence
              attack -= numShips
            } else {
              // If we have more attack capability than they have ships, they're dead.
              damageCounter[target.id] = target.numShips.toDouble()
              attack = 0.0
            }
          }
        }
      }
    }
    if (damageCounter.isNotEmpty()) {
      log("   -- Applying damage...")
      // Now that everyone has attacked, apply the damage.
      for (fleet in star.fleets) {
        val damage = damageCounter[fleet.id] ?: continue
        if (fleet.numShips - damage <= EPSILON) {
          log("      Fleet=%d destroyed (num_ships=%.4f <= damage=%.4f).",
              fleet.id, fleet.numShips, damage)
          fleet.isDestroyed = true
          fleet.numShips = 0f
        } else {
          // They'll be attacking next round (unless their stance is passive).
          var state: Fleet.FLEET_STATE? = Fleet.FLEET_STATE.ATTACKING
          if (fleet.stance == Fleet.FLEET_STANCE.PASSIVE) {
            state = fleet.state
          }
          log("      Fleet=%d numShips=%.8f damage=%.8f state=%s.",
              fleet.id, fleet.numShips, damage, state!!)
          fleet.numShips -= damage.toFloat()
          fleet.state = state
        }
      }
    } else {
      log("   -- No damage to apply.")
    }
  }

  /**
   * Populate the situation reports with the results of combat.
   *
   * @param star The star that this has all occurred on.
   * @param fleetsBefore A collection of the fleets that were on the star before combat started.
   * @param fleetsAfter A collection of the fleets left on the star after combat completed.
   * @param sitReports The situation reports we'll populate with details of the combat.
   */
  private fun populateCombatSitReports(
      star: MutableStar, fleetsBefore: List<Fleet>, fleetsAfter: List<Fleet>,
      sitReports: MutableMap<Long, SituationReport>) {
    // Make sure we have all the empires set up in the situation report to begin with.
    for (fleet in fleetsBefore) {
      if (sitReports[fleet.empire_id] == null) {
        sitReports[fleet.empire_id] = SituationReport(
            empire_id = fleet.empire_id,
            star_id = star.id,
            report_time = System.currentTimeMillis())
      }
    }

    // Now calculate the losses
    val fleetsLost = HashMap<Long, EnumMap<Design.DesignType, Float>>()
    for (fleetBefore in fleetsBefore) {
      var wasDestroyed = true
      var numDestroyed = 0.0f
      for (fleetAfter in fleetsAfter) {
        if (fleetAfter.id == fleetBefore.id) {
          wasDestroyed = false
          numDestroyed = fleetBefore.num_ships - fleetAfter.num_ships
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
        val fleetRecords =
          if (isEnemy) ArrayList(sitReport.fleet_victorious_record)
          else ArrayList(sitReport.fleet_destroyed_record)

        for (lossEntry in combatEntry.value) {
          val fleetRecord = SituationReport.FleetRecord(
              design_type = lossEntry.key,
              num_ships = lossEntry.value)
          fleetRecords.add(fleetRecord)
        }

        sitReports[empireId] =
          if (isEnemy) sitReport.copy(fleet_victorious_record = fleetRecords)
          else sitReport.copy(fleet_destroyed_record = fleetRecords)
      }
    }

    // If it was a move complete record, figure out the triggering fleet and whether it's been
    // destroyed or not.
    // TODO: do the same for build complete records? they result in combat less often, but still...
    for (sitReportEntry in sitReports.entries) {
      val sitReport = sitReportEntry.value
      if (sitReport.move_complete_record != null) {
        var wasDestroyed = true
        for (fleet in fleetsAfter) {
          if (fleet.id == sitReport.move_complete_record.fleet_id) {
            wasDestroyed = false
            break
          }
        }

        sitReports[sitReportEntry.key] =
          sitReport.copy(
            move_complete_record = sitReport.move_complete_record.copy(
              was_destroyed = wasDestroyed))
      }
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
  private fun findTarget(star: MutableStar, fleet: MutableFleet,
                         damageCounter: Map<Long, Double>): MutableFleet? {
    var foundPriority = 9999
    var target: MutableFleet? = null
    for (potentialTarget in star.fleets) {
      // If it's moving we can't attack it.
      if (potentialTarget.state == Fleet.FLEET_STATE.MOVING) {
        continue
      }

      // If it's destroyed, it's not a good target.
      if (potentialTarget.isDestroyed) {
        continue
      }

      // If it's friendly, we can't attack it,
      if (isFriendly(fleet.proto, potentialTarget.proto)) {
        continue
      }

      // If it's already destroyed, we can't attack it.
      val damage = damageCounter[potentialTarget.id]
      if (damage != null && damage >= potentialTarget.numShips) {
        continue
      }

      // If its priority is higher than the one we've already found, then don't bother.
      val design = getDesign(potentialTarget.designType)
      if (design.combat_priority!! > foundPriority) {
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
  private fun simulateEnergyTransports(star: MutableStar) {
    log("Refueling")

    // Find all the energy transport fleets. If there's none we'll try to avoid allocating any extra
    // memory.
    var energyTransportIndices: MutableList<Int?>? = null
    for (i in star.fleets.indices) {
      val fleet = star.fleets[i]
      if (hasEffect(fleet.proto, Design.EffectType.ENERGY_TRANSPORT)) {
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
          if (hasEffect(fleet.proto, Design.EffectType.ENERGY_TRANSPORT)) {
            continue
          }
          if (!isFriendly(fleet.proto, energyTransportFleet.proto)
            || fleet.state != Fleet.FLEET_STATE.IDLE) {
            continue
          }
          val design = getDesign(fleet.designType)
          val requiredEnergy = design.fuel_size!! * fleet.numShips - fleet.fuelAmount
          if (requiredEnergy <= 0) {
            continue
          }
          val availableEnergy = energyTransportFleet.fuelAmount
          if (availableEnergy > requiredEnergy) {
            log(" - filling fleet %d (%.2f required) from energy transport %d: %.2f fuel left in "
                + "transport",
                fleet.id, requiredEnergy, energyTransportFleet.id,
                energyTransportFleet.fuelAmount - requiredEnergy)
            energyTransportFleet.fuelAmount = availableEnergy - requiredEnergy
            star.fleets[energyTransportIndex] = energyTransportFleet
            star.fleets[i].fuelAmount = fleet.fuelAmount + requiredEnergy
          } else {
            log(" - filling fleet %d (with %.2f fuel, %.2f required) from now-empty energy "
                + "transport #%d",
                fleet.id, availableEnergy, requiredEnergy, energyTransportFleet.id)
            star.fleets[energyTransportIndex].fuelAmount = 0f
            star.fleets[i].fuelAmount = fleet.fuelAmount + availableEnergy

            // No point continuing, we're empty.
            break
          }
        }
      }
    }

    // Next, try to refuel everything from our storage(s).
    for (fleet in star.fleets) {
      val design = getDesign(fleet.designType)
      val neededFuelTotal = design.fuel_size!!.toFloat() * fleet.numShips
      if (fleet.fuelAmount < neededFuelTotal) {
        val storageIndex = getStoreIndex(star, fleet.empireId)
        if (storageIndex < 0) {
          // No storages for this empire, nothing to do.
          continue
        }
        val storage = star.empireStores[storageIndex]
        val neededFuelRemaining = neededFuelTotal - fleet.fuelAmount
        val actual = neededFuelRemaining.coerceAtMost(storage.totalEnergy)
        fleet.fuelAmount = fleet.fuelAmount + actual
        storage.totalEnergy = storage.totalEnergy - actual
        log("--- Fleet %d [%s x %.0f] re-fueling: %.2f",
            fleet.id, design.display_name, fleet.numShips, actual)
      }
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
  }
}
