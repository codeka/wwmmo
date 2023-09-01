package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.common.proto.*
import com.google.common.collect.Lists
import java.util.ArrayList
import java.util.Locale


/**
 * Helper for making a star proto with default options if we don't care about all the required
 * fields. Only the ID is required here.
 */
fun makeStar(
  id: Long, name: String = "Test star",
  classification: Star.Classification = Star.Classification.BLUE, size: Int = 1,
  offset_x: Int = 0, offset_y: Int = 0, sector_x: Long = 0L, sector_y: Long = 0L,
  planets: List<Planet> = ArrayList<Planet>(), fleets: List<Fleet> = ArrayList<Fleet>(),
  empire_stores: List<EmpireStorage> = ArrayList<EmpireStorage>()
): Star {
  return Star(
    id, name = name, classification = classification, size = size, offset_x = offset_x,
    offset_y = offset_y, sector_x = sector_x, sector_y = sector_y, planets = planets,
    fleets = fleets, empire_stores = empire_stores)
}

/** Helper for making a planet. */
fun makePlanet(
  index: Int, planetType: Planet.Type = Planet.Type.TERRAN,
  populationCongeniality: Int = 50, farmingCongeniality: Int = 50, miningCongeniality: Int = 50,
  energyCongeniality: Int = 50, colony: Colony? = null): Planet {
  return Planet(index, planet_type = planetType, population_congeniality = populationCongeniality,
    farming_congeniality = farmingCongeniality, mining_congeniality = miningCongeniality,
    energy_congeniality = energyCongeniality, colony = colony)
}

/** Initializes some designs. */
fun initDesign() {
  DesignDefinitions.init(
    Designs(
    designs = Lists.newArrayList(
      Design(
        type = Design.DesignType.COLONY_SHIP,
        design_kind = Design.DesignKind.SHIP,
        display_name = "Colony ship",
        description = "A colony ship",
        image_url = "",
        fuel_size = 800,
        build_cost = Design.BuildCost(
          minerals = 100f,
          population = 120f)),
      Design(
        type = Design.DesignType.SCOUT,
        design_kind = Design.DesignKind.SHIP,
        display_name = "Scout ship",
        description = "A scout ship",
        image_url = "",
        fuel_size = 100,
        build_cost = Design.BuildCost(
          minerals = 10f,
          population = 12f))
    ))
  )
}

class ReportingLogHandler : Simulation.BasicLogHandler() {
  var errors = ArrayList<String>()

  override fun error(format: String, vararg args: Any?) {
    errors.add(String.format(Locale.US, format, args))
    super.error(format, *args)
  }

  override fun write(message: String) {
    super.write(message)
  }
}