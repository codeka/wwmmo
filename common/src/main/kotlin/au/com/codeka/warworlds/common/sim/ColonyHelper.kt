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
    var population: Int = planet.population_congeniality
    if (planet.colony.buildings != null) {
      for (building in planet.colony.buildings) {
        val design = DesignHelper.getDesign(building.design_type)
        val effects = if (building.level > 1) {
           design.upgrades[building.level - 2].effects
          } else {
            design.effect
          }
        for (effect in effects) {
          if (effect.type === Design.EffectType.POPULATION_BOOST) {
            val extraPopulation = effect.minimum.toFloat().coerceAtLeast(population * effect.bonus)
            population += extraPopulation.toInt()
          }
        }
      }
    }
    return population
  }

  /**
   * Get the delta minerals per hour this colony is producing (not counting what's being taken
   * by active build requests).
   */
  fun getDeltaMineralsPerHour(colony: Colony, now: Long): Float {
    var delta: Float = colony.delta_minerals
    for (br in colony.build_requests) {
      if (br.start_time < now && br.end_time > now) {
        delta += br.delta_minerals_per_hour
      }
    }
    return delta
  }
}