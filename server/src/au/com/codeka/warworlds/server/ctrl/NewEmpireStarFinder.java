package au.com.codeka.warworlds.server.ctrl;

/** Find a star which is suitable for a new empire.
 *
 * When a new player joins the game, we want to find a star for their initial
 * colony. We need to choose a star that's close to other players, but not TOO
 * close as to make them an easy target.
 * 
 * @return A \see NewEmpireStarDetails with details of the new star.
 */
public class NewEmpireStarFinder {
    private int mStarID;
    private int mPlanetIndex;

    public int getStarID() {
        return mStarID;
    }
    public int getPlanetIndex() {
        return mPlanetIndex;
    }

    public boolean findStarForNewEmpire() {
        return false; /*
        sector_model = None
        while not sector_model:
          query = (sector_mdl.Sector.all().filter("numColonies <", 30)
                                          .filter("numColonies >=", 1)
                                          .order("numColonies")
                                          .order("distanceToCentre")
                                          .fetch(10))
          models = []
          for s in query:
            key = "%d:%d" % (s.x, s.y)
            if key not in _fullSectors:
              models.append(s)

          if len(models) == 0:
            query = (sector_mdl.Sector.all().filter("numColonies <", 30)
                                            .order("numColonies")
                                            .order("distanceToCentre")
                                            .fetch(10))
            for s in query:
              models.append(s)

          if len(models) == 0:
            # this would happen if there's no sectors loaded that have no colonies... that's bad!!
            logging.warn("Could not find any sectors for new empire, creating some...")
            sectorgen.expandUniverse(immediate=True)
          else:
            index = random.randint(0, len(models) - 1)
            logging.info("Found %d potential sectors, index=%d" % (len(models), index))
            sector_model = models[index]

        # get the existing colonies/fleets in this sector, stars with colonies
        # or fleets are not candidates for new empires
        colonized_stars = []
        for colony_model in mdl.Colony.all().filter("sector", sector_model):
          empire_key = mdl.Colony.empire.get_value_for_datastore(colony_model)
          if empire_key:
            star_key = str(colony_model.key().parent())
            if star_key not in colonized_stars:
              colonized_stars.append(star_key)
        for fleet_model in mdl.Fleet.all().filter("sector", sector_model):
          empire_key = mdl.Fleet.empire.get_value_for_datastore(fleet_model)
          if empire_key:
            star_key = (fleet_model.key().parent())
            if star_key not in colonized_stars:
              colonized_stars.append(star_key)

        stars = []
        for star_model in sector_mdl.Star.all().filter("sector", sector_model):
          star = {"star_model": star_model}
          star["is_colonized"] = (str(star_model.key()) in colonized_stars)
          stars.append(star)

        # Now find a star within that sector. We'll want one with two terran planets
        # with highish population stats, close to the centre of the sector, but far
        # from any existing (non-Native) colonies. We'll score each of the stars
        # based on these factors and then choose the one with the highest score
        starScores = []
        for star in stars:
          if star["is_colonized"]:
            # colonized stars are right out...
            continue
          star_model = star["star_model"]

          centre = sector.SECTOR_SIZE / 2
          distance_to_centre = math.sqrt((star_model.x - centre) * (star_model.x - centre) +
                                         (star_model.y - centre) * (star_model.y - centre))

          # 0 -- 10 (0 is edge of sector, 10 is centre of sector)
          distance_to_centre_score = (centre - distance_to_centre) / (centre / 10)
          if distance_to_centre_score < 1:
            distance_to_centre_score = 1.0

          # figure out the distance to the closest colony
          distance_to_colony_score = 1.0
          distance_to_other_colony = 0
          other_colony = None
          for other_star in stars:
            if other_star["is_colonized"]:
              distance_to_this_colony = math.sqrt(
                  (star_model.x - other_star["star_model"].x) * (star_model.x - other_star["star_model"].x) +
                  (star_model.y - other_star["star_model"].y) * (star_model.y - other_star["star_model"].y))
              if not other_colony or distance_to_this_colony < distance_to_other_colony:
                other_colony = other_star
                distance_to_other_colony = distance_to_this_colony
          if other_colony:
            if distance_to_other_colony < 400:
              distance_to_colony_score = 0
            else:
              distance_to_colony_score = 400 / distance_to_other_colony
            distance_to_colony_score *= distance_to_colony_score

          num_terran_planets = 0
          population_congeniality = 0
          farming_congeniality = 0
          mining_congeniality = 0
          for planet in star_model.planets:
            if planet.planet_type == pb.Planet.TERRAN:
              num_terran_planets += 1
              population_congeniality += planet.population_congeniality
              farming_congeniality += planet.farming_congeniality
              mining_congeniality += planet.mining_congeniality

          planet_score = 0
          if num_terran_planets >= 2:
            planet_score = num_terran_planets

          # if there's no terran planets at all, just ignore this star
          if num_terran_planets == 0:
            continue

          # the average of the congenialities / 100 (should make it approximately 0..10)
          congeniality_score = (population_congeniality / num_terran_planets +
                                farming_congeniality / num_terran_planets +
                                mining_congeniality / num_terran_planets)
          congeniality_score /= 100

          score = (distance_to_centre_score * planet_score * congeniality_score *
                   distance_to_colony_score)
          logging.debug("Star[%s] score=%.2f distance_to_centre_score=%.2f planet_score=%.2f congeniality_score=%.2f distance_to_colony_score=%.2f distance_to_nearest_colony=%.2f" % (
                        star_model.name, score, distance_to_centre_score,
                        planet_score, congeniality_score, distance_to_colony_score,
                        distance_to_other_colony))

          starScores.append((score, star_model))

        # just choose the star with the highest score
        (score, star_model) = sorted(starScores, reverse=True)[0]
        if score < 5:
          logging.warn("Highest score was %.2f, which is too low. Marking this sector full and trying again." % (score))
          key = "%d:%d" % (star_model.sector.x, star_model.sector.y)
          _fullSectors.append(key)
          return findStarForNewEmpire()
        logging.info("Chose star with score=%d (%s)" % (score, star_model.name))

        # next, choose the planet on this star with the highest population congeniality, that'll
        # be the one we start out on
        max_population_congeniality = 0
        planet_index = 0
        for index,planet in enumerate(star_model.planets):
          if planet.planet_type == pb.Planet.TERRAN:
            if planet.population_congeniality > max_population_congeniality:
              planet_index = index + 1
              max_population_congeniality = planet.population_congeniality

        star_key = str(star_model.key())
        return (star_key, planet_index)
*/
    }
}
