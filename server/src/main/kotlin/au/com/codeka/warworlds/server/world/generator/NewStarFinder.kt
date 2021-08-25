package au.com.codeka.warworlds.server.world.generator

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Sector
import au.com.codeka.warworlds.common.proto.SectorCoord
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.store.SectorsStore.SectorState
import au.com.codeka.warworlds.server.world.SectorManager
import au.com.codeka.warworlds.server.world.WatchableObject
import kotlin.math.sqrt

/**
 * Find a star which is suitable for a new empire.
 *
 *
 * When a new player joins the game, we want to find a star for their initial colony. We need to
 * choose a star that's close to other players, but not *too* close as to make them an easy
 * target.
 *
 *
 * Before looking for a brand new star, we'll first look through the abandoned stars for an
 * appropriate one.
 */
class NewStarFinder {
  private val log: Log
  private var coord: SectorCoord?
  lateinit var star: Star
    private set
  var planetIndex = 0
    private set

  @JvmOverloads
  constructor(coord: SectorCoord? = null) {
    log = Log("NewStarFinder")
    this.coord = coord
  }

  constructor(log: Log, coord: SectorCoord?) {
    this.log = log
    this.coord = coord
  }

  fun findStarForNewEmpire(): Boolean {
    if (findAbandonedStar()) {
      return true
    }
    var found = findStar()
    if (!found) {
      // Expand the universe, for good measure.
      SectorGenerator().expandUniverse()

      // And try again.
      found = findStar()
    }
    return found
  }

  /**
   * Look for abandoned stars. We look for stars which are far enough from established empires,
   * but still near the centre of the universe and not *too* far...
   */
  private fun findAbandonedStar(): Boolean { /*
    String sql = "SELECT star_id, empire_id" +
        " FROM abandoned_stars" +
        " WHERE distance_to_non_abandoned_empire > 200" +
        " ORDER BY (distance_to_non_abandoned_empire + distance_to_centre) ASC" +
        " LIMIT 10";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();

      List<Pair<Integer, Integer>> stars = new ArrayList<Pair<Integer, Integer>>();
      while (res.next()) {
        int starID = res.getInt(1);
        int empireID = res.getInt(2);
        stars.add(new Pair<Integer, Integer>(starID, empireID));
      }

      if (stars.size() > 0) {
        Pair<Integer, Integer> starDetails = stars.get(new Random().nextInt(stars.size()));

        // we need to reset the empire on this star so that they move to a different star if the log in again.
        new EmpireController().resetEmpire(starDetails.two, "You have not logged in for a while and your star was reclaimed.");

        starID = starDetails.one;
        findPlanetOnStar(new StarController().getStar(starID));

        // the star is no longer abandoned!
        sql = "DELETE FROM abandoned_stars WHERE star_id = ?";
        try (SqlStmt stmt2 = DB.prepare(sql)) {
          stmt2.setInt(1, starID);
          stmt2.update();
        }
        return true;
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }

    */
    return false
  }

  private fun findStar(): Boolean {
    if (coord == null) {
      coord = DataStore.i.sectors().findSectorByState(SectorState.Empty)
    }
    if (coord == null) {
      log.debug("No empty sector.")
      return false
    }
    val sector: WatchableObject<Sector> = SectorManager.i.getSector(coord!!)
    val star = findHighestScoreStar(sector.get())
    if (star == null) {
      log.debug("No stars found.")
      // TODO: mark the sector as un-inhabitable.
      return false
    }

    // if we get here, then we've found the star. Also find which planet to put the colony on.
    this.star = star
    findPlanetOnStar(star)
    log.debug("Found a star: %d %s (sector: %d,%d)", star.id, star.name, coord!!.x, coord!!.y)
    return true
  }

  /** Find the planet with the highest population congeniality. That's the one.  */
  private fun findPlanetOnStar(star: Star) {
    var highestPopulationCongeniality = 0
    for (planet in star.planets) {
      if (planet.population_congeniality > highestPopulationCongeniality) {
        highestPopulationCongeniality = planet.population_congeniality
        planetIndex = planet.index
      }
    }
  }

  private fun findHighestScoreStar(sector: Sector?): Star? {
    var highestScore = 5.0 // scores lower than 5.0 don't count
    var highestScoreStar: Star? = null
    for (star in sector!!.stars) {
      // ignore colonized stars, they're no good
      if (isColonized(star)) {
        continue
      }

      // similarly, colonies with fleets are right out
      var hasFleets = false
      for (fleet in star.fleets) {
        if (fleet.empire_id != 0L) {
          hasFleets = true
          break
        }
      }
      if (hasFleets) {
        continue
      }
      val score = scoreStar(sector, star)
      if (score > highestScore) {
        highestScore = score
        highestScoreStar = star
      }
    }
    if (highestScoreStar == null) {
      log.error("Highest score star is still null!")
    }
    return highestScoreStar
  }

  private fun isColonized(star: Star): Boolean {
    for (planet in star.planets) {
      val colony = planet.colony ?: continue
      // It's counted as colonized only if it's colonized by a non-native empire.
      if (colony.empire_id != 0L) {
        return true
      }
    }
    return false
  }

  private fun scoreStar(sector: Sector?, star: Star): Double {
    val centre: Int = SectorManager.SECTOR_SIZE / 2
    val distanceToCentre = sqrt((star.offset_x - centre) * (star.offset_x - centre) +
        (star.offset_y - centre) * (star.offset_y - centre).toDouble())
    // 0..10 (0 means the star is on the edge of the sector, 10 means it's the very centre)
    var distanceToCentreScore = (centre - distanceToCentre) / (centre / 10.0)
    if (distanceToCentreScore < 1.0) {
      distanceToCentreScore = 1.0
    }

    // figure out the distance to the closest colonized star and give it a score based on that
    // basically, we want to be about 400 pixels away, no closer but a litter further away is
    // OK as well
    var distanceToOtherColonyScore = 1.0
    var distanceToOtherColony = 0.0
    var otherColony: Star? = null
    for (otherStar in sector!!.stars) {
      if (otherStar.id == star.id) {
        continue
      }
      if (isColonized(otherStar)) {
        val distanceToColony = sqrt((
            (star.offset_x - otherStar.offset_x)
                * (star.offset_x - otherStar.offset_x)
                + (star.offset_y - otherStar.offset_y)
                * (star.offset_y - otherStar.offset_y)).toDouble())
        if (otherColony == null || distanceToColony < distanceToOtherColony) {
          otherColony = otherStar
          distanceToOtherColony = distanceToColony
        }
      }
    }
    if (otherColony != null) {
      if (distanceToOtherColony < 500.0) {
        distanceToOtherColonyScore = 0.0
      } else {
        distanceToOtherColonyScore = 500.0 / distanceToOtherColony
        distanceToOtherColonyScore *= distanceToOtherColonyScore
      }
    }
    var numTerranPlanets = 0.0
    var populationCongeniality = 0.0
    var farmingCongeniality = 0.0
    var miningCongeniality = 0.0
    var energyCongeniality = 0.0
    for (planet in star.planets) {
      if (planet.planet_type == Planet.PLANET_TYPE.TERRAN ||
          planet.planet_type == Planet.PLANET_TYPE.SWAMP ||
          planet.planet_type == Planet.PLANET_TYPE.WATER) {
        numTerranPlanets++
      }
      populationCongeniality += planet.population_congeniality.toDouble()
      farmingCongeniality += planet.farming_congeniality.toDouble()
      miningCongeniality += planet.mining_congeniality.toDouble()
      energyCongeniality += planet.energy_congeniality.toDouble()
    }
    var planetScore = 0.0
    if (numTerranPlanets >= 2) {
      planetScore = numTerranPlanets
    }
    if (numTerranPlanets == 0.0) {
      return 0.0
    }
    var congenialityScore = (populationCongeniality / numTerranPlanets
        + farmingCongeniality / numTerranPlanets
        + miningCongeniality / numTerranPlanets
        + energyCongeniality / numTerranPlanets)
    congenialityScore /= 100.0
    val score = distanceToCentreScore * planetScore * congenialityScore *
        distanceToOtherColonyScore
    log.info("Star[%s] score=%.2f distance_to_centre_score=%.2f planet_score=%.2f "
        + "num_terran_planets=%.0f congeniality_score=%.2f distance_to_colony_score=%.2f "
        + "distance_to_nearest_colony=%.2f",
        star.name, score, distanceToCentreScore, planetScore, numTerranPlanets, congenialityScore,
        distanceToOtherColonyScore, distanceToOtherColony)
    return score
  }
}