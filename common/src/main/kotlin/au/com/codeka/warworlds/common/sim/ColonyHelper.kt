package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.Planet

/** Helper for accessing information about a [Colony]. */
object ColonyHelper {
  fun getMaxPopulation(planet: Planet): Int {
    if (planet.colony == null) {
      return 0
    }
    var population: Int = planet.population_congeniality!!
    for (effect in findEffects(planet, Design.EffectType.POPULATION_BOOST)) {
      val extraPopulation = effect.minimum!!.toFloat().coerceAtLeast(population * effect.bonus!!)
      population += extraPopulation.toInt()
    }
    return population
  }

  /**
   * Gets the farming congeniality for the given planet. If there's a non-null colony, we'll check
   * it for FARMING_BOOST buildings.
   */
  fun getFarmingCongeniality(planet: Planet): Int {
    var congeniality = planet.farming_congeniality!!
    for (effect in findEffects(planet, Design.EffectType.FARMING_BOOST)) {
      val extraCongeniality = effect.minimum!!.coerceAtLeast((congeniality * effect.bonus!!).toInt())
      congeniality += extraCongeniality
    }
    return congeniality
  }

  /**
   * Gets the mining congeniality for the given planet. If there's a non-null colony, we'll check
   * it for MINING_BOOST buildings.
   */
  fun getMiningCongeniality(planet: Planet): Int {
    var congeniality = planet.mining_congeniality!!
    for (effect in findEffects(planet, Design.EffectType.MINING_BOOST)) {
      val extraCongeniality = effect.minimum!!.coerceAtLeast((congeniality * effect.bonus!!).toInt())
      congeniality += extraCongeniality
    }
    return congeniality
  }

  /**
   * Gets the energy congeniality for the given planet. If there's a non-null colony, we'll check
   * it for ENERGY_BOOST buildings.
   */
  fun getEnergyCongeniality(planet: Planet): Int {
    var congeniality = planet.energy_congeniality!!
    for (effect in findEffects(planet, Design.EffectType.ENERGY_BOOST)) {
      val extraCongeniality = effect.minimum!!.coerceAtLeast((congeniality * effect.bonus!!).toInt())
      congeniality += extraCongeniality
    }
    return congeniality
  }

  private fun findEffects(planet: Planet, effectType: Design.EffectType): ArrayList<Design.Effect> {
    val effects = ArrayList<Design.Effect>()
    if (planet.colony?.buildings != null) {
      for (building in planet.colony.buildings) {
        val design = DesignHelper.getDesign(building.design_type!!)
        val designEffects = if (building.level!! > 1) {
          design.upgrades[building.level - 2].effects
        } else {
          design.effect
        }
        for (effect in designEffects) {
          if (effect.type === effectType) {
            effects.add(effect)
          }
        }
      }
    }
    return effects
  }

  /**
   * Get the delta minerals per hour this colony is producing (not counting what's being taken
   * by active build requests).
   */
  fun getDeltaMineralsPerHour(colony: Colony, now: Long): Float {
    var delta: Float = colony.delta_minerals!!
    for (br in colony.build_requests) {
      if (br.start_time!! < now && br.end_time!! > now) {
        delta += br.delta_minerals_per_hour!!
      }
    }
    return delta
  }
}