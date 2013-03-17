package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;

import android.os.Parcel;

/**
 * This class is used to simulate a \c Star. It need to have the same logic as ctrl/simulation.py
 * on the server, and we go to great pains to keep them in sync.
 */
public class Simulation {
    private DateTime mNow;

    private static DateTime year2k = new DateTime(2000, 1, 1, 0, 0);

    public Simulation() {
        mNow = DateTime.now(DateTimeZone.UTC);
    }

    /**
     * Simulate the given star, and make sure it's "current".
     * @param star
     */
    public void simulate(Star star) {
        HashSet<String> empireKeys = new HashSet<String>();
        for (Colony colony : star.getColonies()) {
            if (!empireKeys.contains(colony.getEmpireKey())) {
                empireKeys.add(colony.getEmpireKey());
            }
        }

        // figure out the start time, which is the oldest last_simulation time
        DateTime startTime = getSimulateStartTime(star);
        if (startTime == null) {
            // Nothing worth simulating...
            return;
        }

        DateTime endTime = mNow;

        // if we have less than a few seconds of time to simulate, we'll extend the end time
        // a little to ensure there's no rounding errors and such
        if (endTime.minusSeconds(3).compareTo(startTime) < 0) {
            endTime = startTime.plusSeconds(3);
        }

        // We'll simulate in "prediction mode" for an extra bit of time so that we can get a
        // more accurate estimate of the end time for builds. We won't *record* the population
        // growth and such, just the end time of builds. We'll also record the time that the
        // population drops below a certain threshold so that we can warn the player.
        DateTime predictionTime = endTime.plusHours(24);
        Star predictionStar = null;

        while (true) {
            Duration dt = Duration.standardMinutes(15);
            DateTime stepEndTime = startTime.plus(dt);
            if (stepEndTime.compareTo(endTime) < 0) {
                simulateStepForAllEmpires(dt, startTime, star, empireKeys);
                startTime = stepEndTime;
            } else if (stepEndTime.compareTo(predictionTime) < 0) {
                if (predictionStar == null) {
                    mNow = endTime;
                    dt = new Interval(startTime, endTime).toDuration();
                    if (dt.getMillis() > 1000) {
                        // last little bit of the simulation
                        simulateStepForAllEmpires(dt, startTime, star, empireKeys);
                    }
                    startTime = startTime.plus(dt);

                    Parcel parcel = Parcel.obtain();
                    star.writeToParcel(parcel, 0);
                    parcel.setDataPosition(0);
                    predictionStar = Star.CREATOR.createFromParcel(parcel);
                    dt = Duration.standardMinutes(15);
                }

                simulateStepForAllEmpires(dt, startTime, predictionStar, empireKeys);
                startTime = stepEndTime;
            } else {
                break;
            }
        }

        if (predictionStar != null) {
            // copy the end times for builds from prediction_star_pb
            for (BuildRequest starBuildRequest : star.getBuildRequests()) {
                for (BuildRequest predictedBuildRequest : predictionStar.getBuildRequests()) {
                    if (starBuildRequest.getKey().equals(predictedBuildRequest.getKey())) {
                        starBuildRequest.setEndTime(predictedBuildRequest.getEndTime());
                    }
                }
            }

            //# any fleets that *will be* destroyed, remember the time of their death
            //for fleet_pb in star_pb.fleets:
            //  for prediction_fleet_pb in prediction_star_pb.fleets:
            //    if fleet_pb.key == prediction_fleet_pb.key:
            //      fleet_pb.time_destroyed = prediction_fleet_pb.time_destroyed

            star.setLastSimulation(mNow);
        }
    }

    private DateTime getSimulateStartTime(Star star) {
        DateTime lastSimulation = star.getLastSimulation();
        if (lastSimulation == null) {
            for (Fleet fleet : star.getFleets()) {
                if (lastSimulation == null || fleet.getStateStartTime().compareTo(lastSimulation) < 0) {
                    lastSimulation = fleet.getStateStartTime();
                }
            }
        }
        return lastSimulation;
    }

    private void simulateStepForAllEmpires(Duration dt, DateTime now, Star star, Set<String> empireKeys) {
        for (String empireKey : empireKeys) {
            simulateStep(dt, now, star, empireKey);
        }

        // Don't forget to simulate combat for this step as well (TODO: what to do if combat continues
        // after the prediction phase?)
        simulateCombat(star, now, dt);
    }

    private static boolean equalEmpireKey(String keyOne, String keyTwo) {
        if (keyOne == null && keyTwo == null) {
            return true;
        }
        if (keyOne == null || keyTwo == null) {
            return false;
        }
        return keyOne.equals(keyTwo);
    }

    private void simulateStep(Duration dt, DateTime now, Star star, String empireKey) {
        float totalGoods = 0.0f;
        float totalMinerals = 0.0f;
        float totalPopulation = 0.0f;
        float maxGoods = 500.0f;
        float maxMinerals = 500.0f;

        for (EmpirePresence empire : star.getEmpires()) {
            if (!equalEmpireKey(empire.getEmpireKey(), empireKey)) {
                continue;
            }
            totalGoods = empire.getTotalGoods();
            totalMinerals = empire.getTotalMinerals();
            maxGoods = empire.getMaxGoods();
            maxMinerals = empire.getMaxMinerals();
        }

        float dtInHours = ((float) dt.getMillis()) / (1000.0f * 3600.0f);
        float goodsDeltaPerHour = 0.0f;
        float mineralsDeltaPerHour = 0.0f;

        for (Colony colony : star.getColonies()) {
            if (!equalEmpireKey(colony.getEmpireKey(), empireKey)) {
                continue;
            }

            Planet planet = star.getPlanets()[colony.getPlanetIndex() - 1];

            // calculate the output from farming this turn and add it to the star global
            float goods = colony.getPopulation() * colony.getFarmingFocus() *
                          (planet.getFarmingCongeniality() / 100.0f);
            colony.setGoodsDelta(goods);
            totalGoods += goods * dtInHours;
            goodsDeltaPerHour += goods;

            // calculate the output from mining this turn and add it to the star global
            float minerals = colony.getPopulation() * colony.getMiningFocus() *
                             (planet.getMiningCongeniality() / 100.0f);
            colony.setMineralsDelta(minerals);
            totalMinerals += minerals * dtInHours;
            mineralsDeltaPerHour += minerals;

            totalPopulation += colony.getPopulation();
        }

        // A second loop though the colonies, once the goods/minerals have been calculated. This way,
        // goods minerals are shared between colonies
        for (Colony colony : star.getColonies()) {
            if (!equalEmpireKey(colony.getEmpireKey(), empireKey)) {
                continue;
            }

            ArrayList<BuildRequest> buildRequests = new ArrayList<BuildRequest>();
            for (BuildRequest br : star.getBuildRequests()) {
                if (br.getColonyKey().equals(colony.getKey())) {
                    buildRequests.add(br);
                }
            }

            // not all build requests will be processed this turn. We divide up the population
            // based on the number of ACTUAL build requests they'll be working on this turn
            int numValidBuildRequests = 0;
            for (BuildRequest br : buildRequests) {
                if (br.getStartTime().compareTo(now.plus(dt)) > 0) {
                    continue;
                }

                // the end_time will be accurate, since it'll have been updated last step
                if (br.getEndTime().compareTo(now) < 0 && br.getEndTime().compareTo(year2k) > 0) {
                    continue;
                }

                // as long as it's started but hasn't finished, we'll be working on it this turn
                numValidBuildRequests += 1;
            }

            // If we have pending build requests, we'll have to update them as well
            if (numValidBuildRequests > 0) {
                float totalWorkers = colony.getPopulation() * colony.getConstructionFocus();
                float workersPerBuildRequest = totalWorkers / numValidBuildRequests;

                // OK, we can spare at least ONE population
                if (workersPerBuildRequest < 1.0f) {
                    workersPerBuildRequest = 1.0f;
                }

                for (BuildRequest br : buildRequests) {
                    Design design = DesignManager.getInstance(br.getBuildKind()).getDesign(br.getDesignID());

                    DateTime startTime = br.getStartTime();
                    if (startTime.compareTo(now.plus(dt)) > 0) {
                        continue;
                    }

                    // So the build time the design specifies is the time to build the structure with
                    // 100 workers available. Double the workers and you halve the build time. Halve
                    // the workers and you double the build time.
                    float totalBuildTimeInHours = br.getCount() * design.getBuildCost().getTimeInSeconds() / 3600.0f;
                    totalBuildTimeInHours *= (100.0 / workersPerBuildRequest);

                    // the number of hours of work required, assuming we have all the minerals we need
                    float timeRemainingInHours = (1.0f - br.getProgress(false)) * totalBuildTimeInHours;

                    float dtUsed = dtInHours;
                    if (startTime.compareTo(now) > 0) {
                        Duration startOffset = new Interval(startTime, now).toDuration();
                        dtUsed -= startOffset.getMillis() / (1000.0f * 3600.0f);
                        if (dtUsed > timeRemainingInHours) {
                            dtUsed = timeRemainingInHours;
                        }
                    }

                    // what is the current amount of time we have now as a percentage of the total build
                    // time?
                    float progressThisTurn = dtUsed / totalBuildTimeInHours;
                    if (progressThisTurn <= 0) {
                        timeRemainingInHours = (1.0f - br.getProgress(false)) * totalBuildTimeInHours;
                        DateTime endTime = now.plus((long)(timeRemainingInHours * 3600.0f * 1000.0f));
                        if (br.getEndTime().compareTo(endTime) > 0) {
                            br.setEndTime(endTime);
                        }
                        continue;
                    }

                    // work out how many minerals we require for this turn
                    float mineralsRequired = br.getCount() * design.getBuildCost().getCostInMinerals() * progressThisTurn;
                    if (mineralsRequired > totalMinerals) {
                        // not enough minerals, no progress will be made this turn
                    } else {
                        // awesome, we have enough minerals so we can make some progress. We'll start by
                        // removing the minerals we need from the global pool...
                        totalMinerals -= mineralsRequired;
                        br.setProgress(br.getProgress(false) + progressThisTurn);
                        mineralsDeltaPerHour -= mineralsRequired / dtInHours;
                    }

                    // adjust the end_time for this turn
                    timeRemainingInHours = (1.0f - br.getProgress(false)) * totalBuildTimeInHours;
                    DateTime endTime = now.plus((long)(dtUsed * 1000 * 3600) + (long)(timeRemainingInHours * 1000 * 3600));
                    br.setEndTime(endTime);

                    if (br.getProgress(false) >= 1.0f) {
                        // if we've finished this turn, just set progress
                        br.setProgress(1.0f);
                    }
                }
            }

            // work out the amount of taxes this colony has generated in the last turn
            float taxPerPopulationPerHour = 0.012f;
            float taxThisTurn = taxPerPopulationPerHour * dtInHours * colony.getPopulation();
            colony.setUncollectedTaxes(colony.getUncollectedTaxes() + taxThisTurn);
        }

        // Finally, update the population. The first thing we need to do is evenly distribute goods
        // between all of the colonies.
        float totalGoodsPerHour = totalPopulation / 10.0f;
        float totalGoodsRequired = totalGoodsPerHour * dtInHours;
        goodsDeltaPerHour -= totalGoodsPerHour;

        // If we have more than total_goods_required stored, then we're cool. Otherwise, our population
        // suffers...
        float goodsEfficiency = 1.0f;
        if (totalGoodsRequired > totalGoods && totalGoodsRequired > 0) {
            goodsEfficiency = totalGoods / totalGoodsRequired;
        }

        // subtract all the goods we'll need
        totalGoods -= totalGoodsRequired;
        if (totalGoods < 0.0f) {
            // We've run out of goods! That's bad...
            totalGoods = 0.0f;
        }

        // now loop through the colonies and update the population/goods counter
        for (Colony colony : star.getColonies()) {
            if (!equalEmpireKey(colony.getEmpireKey(), empireKey)) {
                continue;
            }

            float populationIncrease = colony.getPopulation() * colony.getPopulationFocus();
            if (goodsEfficiency >= 1.0f) {
                populationIncrease *= 0.5f;
            } else {
                populationIncrease = colony.getPopulation() * (1.0f - colony.getPopulationFocus());
                populationIncrease *= 0.5f * (goodsEfficiency - 1.0f);
            }

            // if we're increasing population, it slows down the closer you get to the
            // max population. If population is decreasing, it slows down the FURTHER
            // you get.
            float maxFactor = colony.getMaxPopulation();
            if (maxFactor < 10.0f) {
                maxFactor = 10.0f;
            }
            maxFactor = colony.getPopulation() / maxFactor;
            if (maxFactor > 1.0f) {
                // population is bigger than it should be...
                populationIncrease = colony.getPopulation() * (1.0f - colony.getPopulationFocus()) * 0.5f;

                maxFactor -= 1.0f;
                populationIncrease = -maxFactor * populationIncrease;
            }
            if (populationIncrease > 0.0f) {
                maxFactor = 1.0f - maxFactor;
            }

            populationIncrease *= maxFactor;
            colony.setPopulationDelta(populationIncrease);
            populationIncrease *= dtInHours;

            colony.setPopulation(colony.getPopulation() + populationIncrease);
            if (colony.isInCooldown() && colony.getPopulation() < 100.0f) {
                colony.setPopulation(100.0f);
            }
        }

        if (totalGoods > maxGoods) {
            totalGoods = maxGoods;
        }
        if (totalMinerals > maxMinerals) {
            totalMinerals = maxMinerals;
        }

        for (EmpirePresence empire : star.getEmpires()) {
            if (!equalEmpireKey(empire.getEmpireKey(), empireKey)) {
                continue;
            }
            empire.setTotalGoods(totalGoods);
            empire.setTotalMinerals(totalMinerals);
            empire.setDeltaGoodsPerHour(goodsDeltaPerHour);
            empire.setDeltaMineralsPerHour(mineralsDeltaPerHour);
        }
    }

    private void simulateCombat(Star star, DateTime now, Duration dt) {
        // We don't actually simulate combat on the client... todo?
    }
}
