package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.*

class MutableBuilding(private val building: Building) {
  var id = building.id!!

  val designType
    get() = building.design_type!!

  var level = building.level ?: 0

  fun build(): Building {
    return building.copy(
      id = id,
      level = level)
  }
}

class MutableFocus(focus: ColonyFocus) {
  var construction = focus.construction
  var farming = focus.farming
  var mining = focus.mining
  var energy = focus.energy

  fun build(): ColonyFocus {
    return ColonyFocus(
      construction = construction,
      farming = farming,
      mining = mining,
      energy = energy)
  }
}

class MutableBuildRequest(private val buildRequest: BuildRequest) {
  val id
    get() = buildRequest.id!!
  val startTime
    get() = buildRequest.start_time!!
  val designType
    get() = buildRequest.design_type!!

  val count
    get() = buildRequest.count ?: 1

  /** Non-null only if upgrading a building. */
  val buildingId
    get() = buildRequest.building_id

  var progress = buildRequest.progress ?: 0f
  var endTime = buildRequest.end_time ?: Long.MAX_VALUE

  var mineralsDeltaPerHour = buildRequest.delta_minerals_per_hour ?: 0f
  var mineralsEfficiency = buildRequest.minerals_efficiency ?: 0f
  var populationEfficiency = buildRequest.population_efficiency ?: 0f
  var progressPerStep = buildRequest.progress_per_step ?: 0f

  fun build(): BuildRequest {
    return buildRequest.copy(
      progress = progress,
      end_time = endTime,
      delta_minerals_per_hour = mineralsDeltaPerHour,
      minerals_efficiency = mineralsEfficiency,
      population_efficiency = populationEfficiency,
      progress_per_step = progressPerStep)
  }
}

class MutableColony(private val colony: Colony) {
  val id
    get() = colony.id
  val empireId
    get() = colony.empire_id

  var defenceBonus = colony.defence_bonus ?: 0f

  var focus = MutableFocus(colony.focus)

  /** Null if there is no cooldown. */
  var cooldownEndTime = colony.cooldown_end_time

  var buildings = ArrayList(colony.buildings.map { MutableBuilding(it) })
  var buildRequests = ArrayList(colony.build_requests.map { MutableBuildRequest(it) })

  var population = colony.population

  var deltaGoods = colony.delta_goods ?: 0f
  var deltaMinerals = colony.delta_minerals ?: 0f
  var deltaPopulation = colony.delta_population ?: 0f
  var deltaEnergy = colony.delta_energy ?: 0f

  fun build(): Colony {
    return colony.copy(
      focus = focus.build(),
      buildings = buildings.map { it.build() },
      build_requests = buildRequests.map { it.build() },
      defence_bonus = defenceBonus,
      population = population,
      delta_goods = deltaGoods,
      delta_minerals = deltaMinerals,
      delta_population = deltaPopulation,
      delta_energy = deltaEnergy,
      cooldown_end_time = cooldownEndTime)
  }
}

class MutablePlanet(private val planet: Planet) {
  /**
   * Gets the immutable [Planet] we were constructed with. If you've made changes to this planet
   * this will *not* include the changes.
   */
  val proto
    get() = planet

  val index
    get() = planet.index
  val populationCongeniality
    get() = planet.population_congeniality

  var colony: MutableColony? = if (planet.colony == null) null else MutableColony(planet.colony)

  fun build(): Planet {
    return planet.copy(
      colony = colony?.build())
  }
}

class MutableFleet(private val fleet: Fleet) {
  /**
   * Gets the immutable [Fleet] we were constructed with. If you've made changes to this fleet
   * this will *not* include the changes.
   */
  val proto
    get() = fleet
  val empireId
    get() = fleet.empire_id
  val designType
    get() = fleet.design_type!!

  var id = fleet.id

  var state = fleet.state
  var stateStartTime = fleet.state_start_time
  var stance = fleet.stance
  var numShips = fleet.num_ships
  var isDestroyed = fleet.is_destroyed ?: false
  var fuelAmount = fleet.fuel_amount ?: 0f
  var destinationStarId = fleet.destination_star_id
  var eta = fleet.eta

  fun build(): Fleet {
    return fleet.copy(
      id = id,
      state = state,
      state_start_time = stateStartTime,
      stance = stance,
      num_ships = numShips,
      is_destroyed = isDestroyed,
      fuel_amount = fuelAmount,
      destination_star_id = destinationStarId,
      eta = eta)
  }
}

class MutableEmpireStorage(private val empireStore: EmpireStorage) {
  val empireId: Long?
    get() = empireStore.empire_id

  var maxEnergy = empireStore.max_energy ?: 0f
  var maxGoods = empireStore.max_goods ?: 0f
  var maxMinerals = empireStore.max_minerals ?: 0f

  var totalEnergy = empireStore.total_energy ?: 0f
  var totalGoods = empireStore.total_goods ?: 0f
  var totalMinerals = empireStore.total_minerals ?: 0f

  var energyDeltaPerHour = empireStore.energy_delta_per_hour ?: 0f
  var goodsDeltaPerHour = empireStore.goods_delta_per_hour ?: 0f
  var mineralsDeltaPerHour = empireStore.minerals_delta_per_hour ?: 0f

  /** Null if we don't hit zero. */
  var goodsZeroTime = empireStore.goods_zero_time

  fun build(): EmpireStorage {
    return empireStore.copy(
      max_energy = maxEnergy,
      max_goods = maxGoods,
      max_minerals = maxMinerals,
      total_energy = totalEnergy,
      total_goods = totalGoods,
      total_minerals = totalMinerals,
      energy_delta_per_hour = energyDeltaPerHour,
      goods_delta_per_hour = goodsDeltaPerHour,
      minerals_delta_per_hour = mineralsDeltaPerHour,
      goods_zero_time = goodsZeroTime)
  }
}

class MutableCombatReport(combatReport: CombatReport = CombatReport()) {
  var time = combatReport.time ?: 0L

  // Note: we want to make sure our before/after are copies!
  var fleetsBefore = combatReport.fleets_before.map { it.copy() }
  var fleetsAfter = combatReport.fleets_after.map { it.copy() }

  fun build(): CombatReport {
    return CombatReport(
      time = time,
      fleets_before = fleetsBefore,
      fleets_after = fleetsAfter)
  }
}

/**
 * The [Star] protocol buffer is kind of a pain to mutate. This is basically a mirror of [Star] that
 * is fully mutable, for easier simulation, etc.
 */
class MutableStar private constructor(private val star: Star) {
  companion object {
    fun from(star: Star): MutableStar {
      return MutableStar(star)
    }
  }

  val id = star.id
  val sectorX
    get() = star.sector_x
  val sectorY
    get() = star.sector_y
  val offsetX
    get() = star.offset_x
  val offsetY
    get() = star.offset_y

  var name = star.name
  var lastSimulation = star.last_simulation
  var planets = ArrayList(star.planets.map { MutablePlanet(it) })
  var fleets = ArrayList(star.fleets.map { MutableFleet(it) })
  var empireStores = ArrayList(star.empire_stores.map { MutableEmpireStorage(it) })
  var combatReports = ArrayList(star.combat_reports.map { MutableCombatReport(it) })
  var scoutReports = ArrayList(star.scout_reports)
  var nextSimulation = star.next_simulation

  // Re-build this [MutableStar] back into a [Star]
  fun build(): Star {
    return star.copy(
      name = name,
      last_simulation = lastSimulation,
      planets = planets.map { it.build() },
      fleets = fleets.map { it.build() },
      empire_stores = empireStores.map { it.build() },
      combat_reports = combatReports.map { it.build() },
      scout_reports = scoutReports,
      next_simulation = nextSimulation)
  }
}
