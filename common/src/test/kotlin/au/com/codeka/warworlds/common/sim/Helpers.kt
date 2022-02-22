package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.*
import com.google.common.collect.Lists
import java.util.ArrayList


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

/**
 * Initializes some designs.
 */
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
