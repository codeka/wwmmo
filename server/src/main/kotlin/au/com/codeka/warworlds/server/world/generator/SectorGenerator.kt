package au.com.codeka.warworlds.server.world.generator

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.PointCloud.PoissonGenerator
import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Sector
import au.com.codeka.warworlds.common.proto.SectorCoord
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.proto.Star.CLASSIFICATION
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.store.SectorsStore.SectorState
import au.com.codeka.warworlds.server.world.SectorManager
import java.util.*

/**
 * The sector generator is used to expand the universe, and generate bunch of sectors when we run
 * out for new players (or when a player creeps up on the edge of the universe).
 */
class SectorGenerator {
  fun generate(sector: Sector): Sector {
    if (sector.state != SectorState.New.value) {
      return sector
    }
    log.info("Generating sector (%d, %d)...", sector.x, sector.y)
    val coord = SectorCoord(x = sector.x, y = sector.y)
    if (!DataStore.i.sectors().updateSectorState(
            coord, SectorState.New, SectorState.Generating)) {
      log.warning("Could not update sector state to generating, assuming we can't generate there.")
      return sector
    }
    val random = Random(sector.x * 73649274L xor sector.y xor System.currentTimeMillis())
    val points = PoissonGenerator().generate(STAR_DENSITY, STAR_RANDOMNESS, random)
    val stars = ArrayList<Star>()
    for (point in points) {
      stars.add(generateStar(random, coord, point))
    }
    for (star in stars) {
      DataStore.i.stars().put(star.id, star)
    }
    if (!DataStore.i.sectors().updateSectorState(
        coord, SectorState.Generating, SectorState.Empty)) {
      log.warning("Error saving sector state after generating stars.")
    }
    log.info("  sector (${sector.x}, ${sector.y}) generated with ${stars.size} stars.")
    return sector.copy(
        stars = stars,
        state = SectorState.Empty.value)
  }

  /** Expands the universe by (at least) one sector.  */
  fun expandUniverse() {
    expandUniverse(50)
  }

  private fun expandUniverse(num: Int) {
    var numToGenerate = num
    log.debug("Expanding universe by %d", numToGenerate)
    val coords: List<SectorCoord> =
      DataStore.i.sectors().findSectorsByState(SectorState.New, numToGenerate)
    for (coord in coords) {
      generate(Sector(
          x = coord.x,
          y = coord.y,
          state = SectorState.New.value,
          num_colonies = 0))
      numToGenerate--
    }
    if (numToGenerate > 0) {
      DataStore.i.sectors().expandUniverse()
      expandUniverse(numToGenerate)
    }
  }

  private fun generateStar(random: Random, sectorCoord: SectorCoord, point: Vector2): Star {
    val classification = CLASSIFICATION.fromValue(select(random, STAR_TYPE_BONUSES))!!
    val planets = generatePlanets(random, classification)
    return Star(
        id = DataStore.i.seq().nextIdentifier(),
        classification = classification,
        name = NameGenerator().generate(random),
        offset_x = ((SectorManager.SECTOR_SIZE - 64) * point.x).toInt() + 32,
        offset_y = ((SectorManager.SECTOR_SIZE - 64) * point.y).toInt() + 32,
        planets = planets,
        sector_x = sectorCoord.x,
        sector_y = sectorCoord.y,
        size = random.nextInt(8) + 16)
  }

  private fun generatePlanets(random: Random, classification: CLASSIFICATION): ArrayList<Planet> {
    var numPlanets = 0
    while (numPlanets < 2) {
      numPlanets = select(random, PLANET_COUNT_BONUSES)
    }
    val planets = ArrayList<Planet>()
    for (planetIndex in 0 until numPlanets) {
      val bonuses = IntArray(PLANET_TYPE_SLOT_BONUSES[0].size)
      for (i in bonuses.indices) {
        bonuses[i] = PLANET_TYPE_SLOT_BONUSES[planetIndex][i] +
            PLANET_TYPE_STAR_BONUSES[classification.ordinal][i]
      }
      val planetType = select(random, bonuses)
      val populationMultiplier = PLANET_POPULATION_BONUSES[planetType]
      val farmingMultiplier = PLANET_FARMING_BONUSES[planetType]
      val miningMultiplier = PLANET_MINING_BONUSES[planetType]
      val energyMultipler = PLANET_ENERGY_BONUSES[planetType]
      planets.add(Planet(
          index = planetIndex,
          planet_type = Planet.PLANET_TYPE.fromValue(planetType + 1)!!,
          population_congeniality = (normalRandom(random, 1000) * populationMultiplier).toInt(),
          farming_congeniality = (normalRandom(random, 100) * farmingMultiplier).toInt(),
          mining_congeniality = (normalRandom(random, 100) * miningMultiplier).toInt(),
          energy_congeniality = (normalRandom(random, 100) * energyMultipler).toInt()))
    }
    return planets
  }

  /**
   * Selects an index from a list of bonuses.
   *
   *
   * For example, if you pass in [0,0,0,0], then all four indices are equally likely and
   * we will return a value in the range [0,4) with equal probability. If you pass in something
   * like [0,0,30] then the third item has a "bonus" of 30 and is hence 2 is a far more likely
   * result than 0 or 1.
   */
  private fun select(random: Random, bonuses: IntArray): Int {
    val values = IntArray(bonuses.size)
    var total = 0
    for (i in bonuses.indices) {
      val bonus = bonuses[i]
      val n = bonus + normalRandom(random, 100)
      if (n > 0) {
        total += n
        values[i] = n
      } else {
        values[i] = 0
      }
    }
    var randValue = random.nextInt(total)
    for (i in values.indices) {
      randValue -= values[i]
      if (randValue <= 0) {
        return i
      }
    }
    throw RuntimeException("Should not get here!")
  }

  /**
   * Generates a random number that has an approximate normal distribution around the midpoint.
   *
   *
   * For example, if maxValue=100 then you'll most get values around 50 and only occasionally 0
   * or 100. Depending on the number of rounds, the tighter the distribution around the midpoint.
   */
  private fun normalRandom(random: Random, max: Int): Int {
    val rounds = 5
    var n = 0
    val step = max / rounds
    for (i in 0 until rounds) {
      n += random.nextInt(step - 1)
    }
    return n
  }

  companion object {
    private val log = Log("SectorGenerator")
    private const val STAR_DENSITY = 0.18
    private const val STAR_RANDOMNESS = 0.11

    /**
     * This is used to choose a star type at a given point in the map. The order is based on the order
     * of Star.CLASSIFICATION:
     *
     * Blue, White, Orange, Red, Neutron, Black Hole
     */
    private val STAR_TYPE_BONUSES = intArrayOf(30, 40, 50, 40, 30, 0, 0)

    /**
     * Bonuses for generating the number of planets around a star. It's impossible to have 2 or less,
     * 3 is quite unlikely.
     */
    private val PLANET_COUNT_BONUSES = intArrayOf(-9999, -9999, 0, 10, 20, 10, 5, 0)

    /**
     * Planet type bonuses. The bonuses for each entry need to be added to get the "final" bonus
     */
    private val PLANET_TYPE_SLOT_BONUSES =
      arrayOf(
        intArrayOf(-20, 10, 20, -20, -20, 0, 10, 0, -10),
        intArrayOf(-10, 0, 10, -20, 0, 0, 0, 0, -10),
        intArrayOf(0, -10, -10, 0, 0, 0, 0, 0, 20),
        intArrayOf(10, -10, -20, 0, 10, 0, -10, 10, 25),
        intArrayOf(20, -20, -30, -10, 10, 0, -20, 10, 30),
        intArrayOf(20, -20, -40, -10, 0, 0, -30, 0, 5),
        intArrayOf(30, -20, -40, -10, 0, 0, -30, 0, 0))
    private val PLANET_TYPE_STAR_BONUSES =
      arrayOf(
        intArrayOf(-10, 0, 0, -10, 10, -10, 0, 10, 40),
        intArrayOf(-10, -5, -10, -10, 20, -10, 0, 20, 50),
        intArrayOf(-10, -5, -20, -10, 30, -10, 0, 30, 60),
        intArrayOf(-20, -15, -30, -10, 30, -5, 0, 40, 70),
        intArrayOf(-20, -15, -40, -10, 20, -5, 0, 40, 80),
        intArrayOf(-30, 20, 10, -10, -10, 0, -10, -10, -30),
        intArrayOf(-30, 30, 20, -10, -20, 0, -10, -10, -30))

    // Planet population is calculated based on the size of the planet (usually, the bigger
    // the planet, the higher the potential population) but also the following bonuses are
    // applied.
    private val PLANET_POPULATION_BONUSES = doubleArrayOf(
        // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
        0.4, 0.4, 0.4, 0.0, 1.1, 0.6, 0.9, 0.9, 1.5
    )
    private val PLANET_FARMING_BONUSES = doubleArrayOf(
        // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
        0.4, 0.2, 0.2, 0.0, 1.4, 0.4, 0.6, 1.0, 1.2
    )
    private val PLANET_MINING_BONUSES = doubleArrayOf(
        // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
        0.8, 1.5, 1.0, 2.5, 0.3, 0.4, 0.6, 0.6, 0.8
    )
    private val PLANET_ENERGY_BONUSES = doubleArrayOf(
        // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
        0.8, 2.0, 2.5, 0.1, 0.8, 1.0, 0.3, 0.5, 1.0
    )
  }
}