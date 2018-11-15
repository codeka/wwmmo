package au.com.codeka.warworlds.server.world.generator;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.store.SectorsStore;
import au.com.codeka.warworlds.server.world.SectorManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Find a star which is suitable for a new empire.
 *
 * <p>When a new player joins the game, we want to find a star for their initial colony. We need to
 * choose a star that's close to other players, but not <i>too</i> close as to make them an easy
 * target.
 *
 * <p>Before looking for a brand new star, we'll first look through the abandoned stars for an
 * appropriate one.
 */
public class NewStarFinder {
  private final Log log;

  @Nullable private SectorCoord coord;
  private Star star;
  private int planetIndex;

  public Star getStar() {
    return star;
  }
  public int getPlanetIndex() {
    return planetIndex;
  }

  public NewStarFinder() {
    this(null);
  }

  public NewStarFinder(@Nullable SectorCoord coord) {
    this.log = new Log("NewStarFinder");
    this.coord = coord;
  }

  public NewStarFinder(Log log, @Nullable SectorCoord coord) {
    this.log = log;
    this.coord = coord;
  }

  public boolean findStarForNewEmpire() {
    if (findAbandonedStar()) {
      return true;
    }

    boolean found = findStar();
    if (!found) {
      // Expand the universe, for good measure.
      new SectorGenerator().expandUniverse();

      // And try again.
      found = findStar();
    }

    if (found) {
      // Make sure the coord there isn't counted as being empty any more.
      DataStore.i.sectors().updateSectorState(coord, SectorsStore.SectorState.NonEmpty);
    }
    return found;
  }

  /**
   * Look for abandoned stars. We look for stars which are far enough from established empires, but still
   * near the centre of the universe and not *too* far...
   */
  private boolean findAbandonedStar() {/*
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

    */return false;
  }

  private boolean findStar() {
    if (coord == null) {
      coord = DataStore.i.sectors().findSectorByState(SectorsStore.SectorState.Empty);
    }
    if (coord == null) {
      log.debug("No empty sector.");
      return false;
    }

    WatchableObject<Sector> sector = SectorManager.i.getSector(coord);
    Star star = findHighestScoreStar(sector.get());
    if (star == null) {
      log.debug("No stars found.");
      return false;
    }

    // if we get here, then we've found the star. Also find which planet to put the colony on.
    this.star = star;
    findPlanetOnStar(star);
    log.debug("Found a star: %d %s (sector: %d,%d)", star.id, star.name, coord.x, coord.y);
    return true;
  }

  /** Find the planet with the highest population congeniality. That's the one. */
  private void findPlanetOnStar(Star star) {
    int highestPopulationCongeniality = 0;
    for (Planet planet : star.planets) {
      if (planet.population_congeniality > highestPopulationCongeniality) {
        highestPopulationCongeniality = planet.population_congeniality;
        planetIndex = planet.index;
      }
    }
  }

  private Star findHighestScoreStar(Sector sector) {
    double highestScore = 5.0; // scores lower than 5.0 don't count
    Star highestScoreStar = null;

    for (Star star : sector.stars) {
      // ignore colonized stars, they're no good
      if (isColonized(star)) {
        continue;
      }

      // similarly, colonies with fleets are right out
      boolean hasFleets = false;
      for (Fleet fleet : star.fleets) {
        if (fleet.empire_id != null) {
          hasFleets = true;
          break;
        }
      }
      if (hasFleets) {
        continue;
      }

      double score = scoreStar(sector, star);
      if (score > highestScore) {
        highestScore = score;
        highestScoreStar = star;
      }
    }

    if (highestScoreStar == null) {
      log.error("Highest score star is still null!");
    }
    return highestScoreStar;
  }

  private boolean isColonized(Star star) {
    for (Planet planet : star.planets) {
      // It's counted as colonized only if it's colonized by a non-native empire.
      if (planet.colony != null && planet.colony.empire_id != null) {
        return true;
      }
    }
    return false;
  }

  private double scoreStar(Sector sector, Star star) {
    int centre = SectorManager.SECTOR_SIZE / 2;
    double distanceToCentre = Math.sqrt((star.offset_x - centre) * (star.offset_x - centre) +
        (star.offset_y - centre) * (star.offset_y - centre));
    // 0..10 (0 means the star is on the edge of the sector, 10 means it's the very centre)
    double distanceToCentreScore = (centre - distanceToCentre) / (centre / 10.0);
    if (distanceToCentreScore < 1.0) {
      distanceToCentreScore = 1.0;
    }

    // figure out the distance to the closest colonized star and give it a score based on that
    // basically, we want to be about 400 pixels away, no closer but a litter further away is
    // OK as well
    double distanceToOtherColonyScore = 1.0;
    double distanceToOtherColony = 0.0;
    Star otherColony = null;
    for (Star otherStar : sector.stars) {
      if (otherStar.id.equals(star.id)) {
        continue;
      }
      if (isColonized(otherStar)) {
        double distanceToColony = Math.sqrt(
            (star.offset_x - otherStar.offset_x) * (star.offset_x - otherStar.offset_x)
                + (star.offset_y - otherStar.offset_y) * (star.offset_y - otherStar.offset_y));
        if (otherColony == null || distanceToColony < distanceToOtherColony) {
          otherColony = otherStar;
          distanceToOtherColony = distanceToColony;
        }
      }
    }
    if (otherColony != null) {
      if (distanceToOtherColony < 500.0) {
        distanceToOtherColonyScore = 0.0;
      } else {
        distanceToOtherColonyScore = 500.0 / distanceToOtherColony;
        distanceToOtherColonyScore *= distanceToOtherColonyScore;
      }
    }

    double numTerranPlanets = 0.0;
    double populationCongeniality = 0.0;
    double farmingCongeniality = 0.0;
    double miningCongeniality = 0.0;
    double energyCongeniality = 0.0;
    for (Planet planet : star.planets) {
      if (planet.planet_type.equals(Planet.PLANET_TYPE.TERRAN)
          || planet.planet_type.equals(Planet.PLANET_TYPE.SWAMP)
          || planet.planet_type.equals(Planet.PLANET_TYPE.WATER)) {
        numTerranPlanets ++;
      }
      populationCongeniality += planet.population_congeniality;
      farmingCongeniality += planet.farming_congeniality;
      miningCongeniality += planet.mining_congeniality;
      energyCongeniality += planet.energy_congeniality;
    }
    double planetScore = 0.0;
    if (numTerranPlanets >= 2) {
      planetScore = numTerranPlanets;
    }
    if (numTerranPlanets == 0) {
      return 0.0;
    }

    double congenialityScore = (populationCongeniality / numTerranPlanets)
        + (farmingCongeniality / numTerranPlanets)
        + (miningCongeniality / numTerranPlanets)
        + (energyCongeniality / numTerranPlanets);
    congenialityScore /= 100.0;

    double score = (distanceToCentreScore * planetScore * congenialityScore *
        distanceToOtherColonyScore);

    log.info("Star[%s] score=%.2f distance_to_centre_score=%.2f planet_score=%.2f "
        + "num_terran_planets=%.0f congeniality_score=%.2f distance_to_colony_score=%.2f "
        + "distance_to_nearest_colony=%.2f",
        star.name, score, distanceToCentreScore, planetScore, numTerranPlanets, congenialityScore,
        distanceToOtherColonyScore, distanceToOtherColony);
    return score;
  }
}

