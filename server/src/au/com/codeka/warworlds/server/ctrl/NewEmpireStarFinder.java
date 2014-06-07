package au.com.codeka.warworlds.server.ctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BaseSector;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

/** Find a star which is suitable for a new empire.
 *
 * When a new player joins the game, we want to find a star for their initial
 * colony. We need to choose a star that's close to other players, but not TOO
 * close as to make them an easy target.
 * 
 * Before looking for a brand new star, we'll first look through the abandoned stars
 * for an approprite one.
 * 
 * @return A \see NewEmpireStarDetails with details of the new star.
 */
public class NewEmpireStarFinder {
    private final Logger log = LoggerFactory.getLogger(NewEmpireStarFinder.class);
    private int mStarID;
    private int mPlanetIndex;

    public int getStarID() {
        return mStarID;
    }
    public int getPlanetIndex() {
        return mPlanetIndex;
    }

    public boolean findStarForNewEmpire() throws RequestException {
        if (findAbandonedStar()) {
            return true;
        }

        if (!findStarForNewEmpire(true)) {
            if (!findStarForNewEmpire(false)) {
                new SectorGenerator().expandUniverse();
                return findStarForNewEmpire(false);
            }
        }

        return true;
    }

    /**
     * Look for abandoned stars. We look for stars which are far enough from established empires, but still
     * near the centre of the universe and not *too* far...
     */
    private boolean findAbandonedStar() throws RequestException {
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

                mStarID = starDetails.one;
                findPlanetOnStar(new StarController().getStar(mStarID));

                // the star is no longer abandoned!
                sql = "DELETE FROM abandoned_stars WHERE star_id = ?";
                try (SqlStmt stmt2 = DB.prepare(sql)) {
                    stmt2.setInt(1, mStarID);
                    stmt2.update();
                }
                return true;
            }
        } catch (Exception e) {
            throw new RequestException(e);
        }

        return false;
    }

    private boolean findStarForNewEmpire(boolean allowEmptySectors) throws RequestException {
        ArrayList<Integer> sectorIds = findSectors(allowEmptySectors);
        for (int sectorId : sectorIds) {
            Sector sector = new SectorController().getSector(sectorId);
            Star star = findHighestScoreStar(sector);
            if (star == null) {
                continue;
            }

            // if we get here, then we've found the star. Also find which planet to
            // put the colony on.
            mStarID = star.getID();
            findPlanetOnStar(star);

            return true;
        }

        return false;
    }

    private void findPlanetOnStar(Star star) {
        int highestPopulationCongeniality = 0;
        for (BasePlanet planet : star.getPlanets()) {
            if (planet.getPopulationCongeniality() > highestPopulationCongeniality) {
                highestPopulationCongeniality = planet.getPopulationCongeniality();
                mPlanetIndex = planet.getIndex();
            }
        }
    }

    private Star findHighestScoreStar(Sector sector) {
        double highestScore = 5.0; // scores lower than 5.0 don't count
        Star highestScoreStar = null;

        for (BaseStar star : sector.getStars()) {
            // ignore colonized stars, they're no good
            if (isColonized(star)) {
                continue;
            }

            // similarly, colonies with fleets are right out
            boolean hasFleets = false;
            for (BaseFleet fleet : star.getFleets()) {
                if (fleet.getEmpireKey() != null) {
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
                highestScoreStar = (Star) star;
            }
        }

        return highestScoreStar;
    }

    private boolean isColonized(BaseStar star) {
        for (BaseColony colony : star.getColonies()) {
            if (colony.getEmpireKey() != null) {
                return true;
            }
        }
        return false;
    }

    private double scoreStar(Sector sector, BaseStar star) {
        int centre = BaseSector.SECTOR_SIZE / 2;
        double distanceToCentre = Math.sqrt((star.getOffsetX() - centre) * (star.getOffsetX() - centre) +
                                            (star.getOffsetY() - centre) * (star.getOffsetY() - centre));
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
        BaseStar otherColony = null;
        for (BaseStar otherStar : sector.getStars()) {
            if (otherStar.getKey().equals(star.getKey())) {
                continue;
            }
            if (isColonized(otherStar)) {
                double distanceToColony = Math.sqrt(
                        (star.getOffsetX() - otherStar.getOffsetX()) * (star.getOffsetX() - otherStar.getOffsetX()) +
                        (star.getOffsetY() - otherStar.getOffsetY()) * (star.getOffsetY() - otherStar.getOffsetY()));
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
        for (BasePlanet planet : star.getPlanets()) {
            if (planet.getPlanetType().getInternalName().equals("terran") ||
                planet.getPlanetType().getInternalName().equals("swamp") ||
                planet.getPlanetType().getInternalName().equals("water")) {
                numTerranPlanets ++;
            }
            populationCongeniality += planet.getPopulationCongeniality();
            farmingCongeniality += planet.getFarmingCongeniality();
            miningCongeniality += planet.getMiningCongeniality();
        }
        double planetScore = 0.0;
        if (numTerranPlanets >= 2) {
            planetScore = numTerranPlanets;
        }
        if (numTerranPlanets == 0) {
            return 0.0;
        }

        double congenialityScore = (populationCongeniality / numTerranPlanets) +
                                   (farmingCongeniality / numTerranPlanets) +
                                   (miningCongeniality / numTerranPlanets);
        congenialityScore /= 100.0;

        double score = (distanceToCentreScore * planetScore * congenialityScore *
                        distanceToOtherColonyScore);

        log.info(String.format(Locale.ENGLISH,
                "Star[%s] score=%.2f distance_to_centre_score=%.2f planet_score=%.2f num_terran_planets=%.0f congeniality_score=%.2f distance_to_colony_score=%.2f distance_to_nearest_colony=%.2f",
                star.getName(), score, distanceToCentreScore,
                planetScore, numTerranPlanets, congenialityScore, distanceToOtherColonyScore,
                distanceToOtherColony));

        return score;
    }

    private ArrayList<Integer> findSectors(boolean tryNonEmptyFirst) throws RequestException {
        String sql = "SELECT id FROM sectors WHERE num_colonies = 0" +
                " ORDER BY distance_to_centre ASC";
        ArrayList<Integer> ids = new ArrayList<Integer>();
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                ids.add(res.getInt(1));
                if (ids.size() > 10) {
                    break;
                }
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }
        if (!ids.isEmpty()) {
            return ids;
        }

        // if we get here, the universe needs to be expanded by a bit so we have some new sectors
        // to colonies...
        new SectorGenerator().expandUniverse();
        return findSectors(tryNonEmptyFirst);
    }
}
