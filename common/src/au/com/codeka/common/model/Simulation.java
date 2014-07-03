package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Seconds;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseFleet.Stance;


/**
 * This class is used to simulate a \c Star. It need to have the same logic as ctrl/simulation.py
 * on the server, and we go to great pains to keep them in sync.
 */
public class Simulation {
    private LogHandler mLogHandler;
    private DateTime mNow;

    private static boolean sDebug = false;
    private static int sNumSimulations;
    private static DateTime year2k = new DateTime(2000, 1, 1, 0, 0);

    public Simulation() {
        mNow = DateTime.now(DateTimeZone.UTC);
        if (sDebug) {
            mLogHandler = new BasicLogHandler();
        }
    }
    public Simulation(LogHandler log) {
        mNow = DateTime.now(DateTimeZone.UTC);
        mLogHandler = log;
    }
    public Simulation(DateTime now, LogHandler log) {
        mNow = now;
        mLogHandler = log;
    }

    public static int getNumRunningSimulations() {
        return sNumSimulations;
    }

    protected void log(String message) {
        if (mLogHandler != null) {
            mLogHandler.log(message);
        }
    }

    /**
     * Simulate the given star, and make sure it's "current".
     * @param star
     */
    public void simulate(BaseStar star) {
        sNumSimulations ++;
        log(String.format("Begin simulation for '%s'", star.getName()));

        HashSet<String> empireKeys = new HashSet<String>();
        for (BaseColony colony : star.getColonies()) {
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
        BaseStar predictionStar = null;

        while (true) {
            Duration dt = Duration.standardMinutes(15);
            DateTime stepEndTime = startTime.plus(dt);
            if (stepEndTime.compareTo(endTime) < 0) {
                simulateStepForAllEmpires(dt, startTime, star, empireKeys);
                startTime = stepEndTime;
            } else if (stepEndTime.compareTo(predictionTime) < 0) {
                if (predictionStar == null) {
                    log("--------------------------------------------------");
                    log("Prediction phase beginning...");
                    log("--------------------------------------------------");
                    mNow = endTime;
                    dt = new Interval(startTime, endTime).toDuration();
                    if (dt.getMillis() > 1000) {
                        // last little bit of the simulation
                        simulateStepForAllEmpires(dt, startTime, star, empireKeys);
                    }
                    startTime = startTime.plus(dt);

                    predictionStar = star.clone();
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
            for (BaseBuildRequest starBuildRequest : star.getBuildRequests()) {
                for (BaseBuildRequest predictedBuildRequest : predictionStar.getBuildRequests()) {
                    if (starBuildRequest.getKey().equals(predictedBuildRequest.getKey())) {
                        starBuildRequest.setEndTime(predictedBuildRequest.getEndTime());
                    }
                }
            }

            // any fleets that *will be* destroyed, remember the time of their death
            for (BaseFleet fleet : star.getFleets()) {
                for (BaseFleet predictedFleet : predictionStar.getFleets()) {
                    if (fleet.getKey().equals(predictedFleet.getKey())) {
                        log(String.format("Fleet #%s updating timeDestroyed to: %s", fleet.getKey(), predictedFleet.getTimeDestroyed()));
                        fleet.setTimeDestroyed(predictedFleet.getTimeDestroyed());
                    }
                }
            }

            // if the empire is going to run out of resources, save that time as well.
            for (BaseEmpirePresence empirePresence : star.getEmpirePresences()) {
                for (BaseEmpirePresence predictedEmpirePresence : predictionStar.getEmpirePresences()) {
                    if (empirePresence.getKey().equals(predictedEmpirePresence.getKey())) {
                        empirePresence.setGoodsZeroTime(predictedEmpirePresence.getGoodsZeroTime());
                    }
                }
            }

            // also, the prediction combat report (if any) is the one to use
            star.setCombatReport(predictionStar.getCombatReport());

            star.setLastSimulation(endTime);
        }

        sNumSimulations --;
    }

    private DateTime getSimulateStartTime(BaseStar star) {
        DateTime lastSimulation = star.getLastSimulation();
        if (lastSimulation == null) {
            for (BaseFleet fleet : star.getFleets()) {
                if (lastSimulation == null || fleet.getStateStartTime().compareTo(lastSimulation) < 0) {
                    lastSimulation = fleet.getStateStartTime();
                }
            }
        }

        // if there's only native colonies, don't bother simulating from more than
        // 24 hours ago. The native colonies will generally be in a steady state
        DateTime oneDayAgo = DateTime.now().minusHours(24);
        if (lastSimulation != null && lastSimulation.isBefore(oneDayAgo)) {
            log("Last simulation more than on day ago, checking whether there are any non-native colonies.");
            boolean onlyNativeColonies = true;
            for (BaseColony baseColony : star.getColonies()) {
                if (baseColony.getEmpireKey() != null) {
                    onlyNativeColonies = false;
                }
            }
            for (BaseFleet baseFleet : star.getFleets()) {
                if (baseFleet.getEmpireKey() != null) {
                    onlyNativeColonies = false;
                }
            }
            if (onlyNativeColonies) {
                log("No non-native colonies detected, simulating only 24 hours in the past.");
                lastSimulation = oneDayAgo;
            }
        }

        return lastSimulation;
    }

    private void simulateStepForAllEmpires(Duration dt, DateTime now, BaseStar star, Set<String> empireKeys) {
        log(String.format("- Step [dt=%.2f hrs] [now=%s]", (float)(dt.toStandardSeconds().getSeconds()) / 3600.0f, now));
        for (String empireKey : empireKeys) {
            log(String.format("-- Empire [%s]", empireKey == null ? "Native" : empireKey));
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

    private void simulateStep(Duration dt, DateTime now, BaseStar star, String empireKey) {
        float totalGoods = 50.0f;
        float totalMinerals = 50.0f;
        float totalPopulation = 0.0f;
        float maxGoods = 50.0f;
        float maxMinerals = 50.0f;
        float totalTaxPerHour = 0.0f;

        BaseEmpirePresence empire = null;
        for (BaseEmpirePresence e : star.getEmpires()) {
            if (!equalEmpireKey(e.getEmpireKey(), empireKey)) {
                continue;
            }
            empire = e;
            totalGoods = empire.getTotalGoods();
            totalMinerals = empire.getTotalMinerals();
            maxGoods = empire.getMaxGoods();
            maxMinerals = empire.getMaxMinerals();
        }

        float dtInHours = ((float) dt.getMillis()) / (1000.0f * 3600.0f);
        float goodsDeltaPerHour = 0.0f;
        float mineralsDeltaPerHour = 0.0f;

        for (BaseColony colony : star.getColonies()) {
            if (!equalEmpireKey(colony.getEmpireKey(), empireKey)) {
                continue;
            }

            log(String.format("--- Colony [planetIndex=%d] [population=%.2f]",
                    colony.getPlanetIndex(), colony.getPopulation()));
            BasePlanet planet = star.getPlanets()[colony.getPlanetIndex() - 1];

            // calculate the output from farming this turn and add it to the star global
            float goods = colony.getPopulation() * colony.getFarmingFocus() *
                          (planet.getFarmingCongeniality() / 100.0f);
            colony.setGoodsDelta(goods);
            totalGoods += goods * dtInHours;
            goodsDeltaPerHour += goods;
            log(String.format("    Goods: [delta=%.2f / hr] [this turn=%.2f]", goods, goods * dtInHours));

            // calculate the output from mining this turn and add it to the star global
            float minerals = colony.getPopulation() * colony.getMiningFocus() *
                             (planet.getMiningCongeniality() / 100.0f);
            colony.setMineralsDelta(minerals);
            totalMinerals += minerals * dtInHours;
            mineralsDeltaPerHour += minerals;
            log(String.format("    Minerals: [delta=%.2f / hr] [this turn=%.2f]", goods, goods * dtInHours));

            totalPopulation += colony.getPopulation();

            // work out the amount of taxes this colony has generated in the last turn
            float taxPerPopulationPerHour = 0.012f;
            float taxPerHour = taxPerPopulationPerHour * colony.getPopulation();
            float taxThisTurn = taxPerHour * dtInHours;
            log(String.format("    Taxes %.2f + %.2f = %.2f uncollected", colony.getUncollectedTaxes(), taxThisTurn, colony.getUncollectedTaxes() + taxThisTurn));
            totalTaxPerHour += taxPerHour;
            colony.setUncollectedTaxes(colony.getUncollectedTaxes() + taxThisTurn);
        }

        // A second loop though the colonies, once the goods/minerals have been calculated. This way,
        // goods minerals are shared between colonies
        for (BaseColony colony : star.getColonies()) {
            if (!equalEmpireKey(colony.getEmpireKey(), empireKey)) {
                continue;
            }

            ArrayList<BaseBuildRequest> buildRequests = new ArrayList<BaseBuildRequest>();
            for (BaseBuildRequest br : star.getBuildRequests()) {
                if (br.getColonyKey().equals(colony.getKey())) {
                    buildRequests.add(br);
                }
            }

            // not all build requests will be processed this turn. We divide up the population
            // based on the number of ACTUAL build requests they'll be working on this turn
            int numValidBuildRequests = 0;
            for (BaseBuildRequest br : buildRequests) {
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
                log(String.format("--- Building [buildRequests=%d] [planetIndex=%d] [totalWorker=%.2f]",
                        numValidBuildRequests, colony.getPlanetIndex(), totalWorkers));

                // OK, we can spare at least ONE population
                if (workersPerBuildRequest < 1.0f) {
                    workersPerBuildRequest = 1.0f;
                }

                // divide the minerals up per build request, so they each get a share. I'm not sure
                // if we should portion minerals out by how 'big' the build request is, but we'll
                // see how this goes initially
                float mineralsPerBuildRequest = totalMinerals / numValidBuildRequests;

                for (BaseBuildRequest br : buildRequests) {
                    Design design = BaseDesignManager.i.getDesign(br.getDesignKind(), br.getDesignID());
                    log(String.format("---- Building [design=%s %s] [count=%d]",
                            br.getDesignKind(), br.getDesignID(), br.getCount()));

                    DateTime startTime = br.getStartTime();
                    if (startTime.compareTo(now.plus(dt)) > 0) {
                        continue;
                    }

                    // the build cost is defined by the original design, or possibly by the upgrade if that
                    // is what it is.
                    Design.BuildCost buildCost = design.getBuildCost();
                    if (br.mExistingFleetID != null) {
                        ShipDesign shipDesign = (ShipDesign) design;
                        ShipDesign.Upgrade upgrade = shipDesign.getUpgrade(br.getUpgradeID());
                        buildCost = upgrade.getBuildCost();
                    }

                    // So the build time the design specifies is the time to build the structure with
                    // 100 workers available. Double the workers and you halve the build time. Halve
                    // the workers and you double the build time.
                    float totalBuildTimeInHours = br.getCount() * buildCost.getTimeInSeconds() / 3600.0f;
                    totalBuildTimeInHours *= (100.0 / workersPerBuildRequest);

                    // the number of hours of work required, assuming we have all the minerals we need
                    float timeRemainingInHours = (1.0f - br.getProgress(false)) * totalBuildTimeInHours;
                    if (timeRemainingInHours < (10.0f / 3600.0f)) {
                        // if there's less than 10 seconds to go, just say it's done now.
                        timeRemainingInHours = 0.0f;
                    }
                    log(String.format("     Time [total=%.2f hrs] [remaining=%.2f hrs]",
                            totalBuildTimeInHours, timeRemainingInHours));

                    float dtUsed = dtInHours;
                    if (startTime.isAfter(now)) {
                        Duration startOffset = new Interval(now, startTime).toDuration();
                        dtUsed -= startOffset.getMillis() / (1000.0f * 3600.0f);
                    }
                    if (dtUsed > timeRemainingInHours) {
                        dtUsed = timeRemainingInHours;
                    }

                    // what is the current amount of time we have now as a percentage of the total build
                    // time?
                    float progressThisTurn = dtUsed / totalBuildTimeInHours;
                    if (progressThisTurn <= 0) {
                        DateTime endTime;
                        timeRemainingInHours = (1.0f - br.getProgress(false)) * totalBuildTimeInHours;
                        if (timeRemainingInHours < (10.0f / 3600.0f)) {
                            endTime = now;
                        } else {
                            endTime = now.plus((long)(timeRemainingInHours * 3600.0f * 1000.0f));
                        }
                        if (br.getEndTime().compareTo(endTime) > 0) {
                            br.setEndTime(endTime);
                        }
                        continue;
                    }

                    // work out how many minerals we require for this turn
                    float mineralsRequired = br.getCount() * buildCost.getCostInMinerals() * progressThisTurn;
                    if (mineralsRequired > mineralsPerBuildRequest) {
                        // if we don't have enough minerals, we'll just do a percentage of the work
                        // this turn
                        totalMinerals -= mineralsPerBuildRequest;
                        float percentMineralsAvailable = mineralsPerBuildRequest / mineralsRequired;
                        br.setProgress(br.getProgress(false) + (progressThisTurn * percentMineralsAvailable));
                        log(String.format("     Progress %.4f%% + %.4f%% (this turn, adjusted - %.4f%% originally) ",
                            br.getProgress(false) * 100.0f,
                            progressThisTurn * percentMineralsAvailable * 100.0f,
                            progressThisTurn * 100.0f));
                    } else {
                        // awesome, we have enough minerals so we can make some progress. We'll start by
                        // removing the minerals we need from the global pool...
                        totalMinerals -= mineralsRequired;
                        br.setProgress(br.getProgress(false) + progressThisTurn);
                        log(String.format("     Progress %.4f%% + %.4f%% (this turn)",
                            br.getProgress(false) * 100.0f, progressThisTurn * 100.0f));
                    }
                    mineralsDeltaPerHour -= mineralsRequired / dtInHours;
                    log(String.format("     Minerals [required=%.2f] [available=%.2f] [available per build=%.2f]",
                            mineralsRequired, totalMinerals, mineralsPerBuildRequest));

                    // adjust the end_time for this turn
                    timeRemainingInHours = (1.0f - br.getProgress(false)) * totalBuildTimeInHours;
                    if (timeRemainingInHours > 100000) {
                        // this is waaaaaay too long! it's basically never going to finish, but cap it to
                        // avoid overflow errors.
                        timeRemainingInHours = 100000;
                    }
                    DateTime endTime = now.plus((long)(dtUsed * 1000 * 3600) + (long)(timeRemainingInHours * 1000 * 3600));
                    br.setEndTime(endTime);
                    log(String.format("     End Time: %s (%.2f hrs)", endTime, Seconds.secondsBetween(now, endTime).getSeconds() / 3600.0f));

                    if (br.getProgress(false) >= 1.0f) {
                        // if we've finished this turn, just set progress
                        br.setProgress(1.0f);
                    }
                }
            }
        }

        // Finally, update the population. The first thing we need to do is evenly distribute goods
        // between all of the colonies.
        float totalGoodsPerHour = totalPopulation / 10.0f;
        if (totalPopulation > 0.0001f && totalGoodsPerHour < 10.0f) {
            totalGoodsPerHour = 10.0f;
        }
        float totalGoodsRequired = totalGoodsPerHour * dtInHours;
        goodsDeltaPerHour -= totalGoodsPerHour;

        // If we have more than total_goods_required stored, then we're cool. Otherwise, our population
        // suffers...
        float goodsEfficiency = 1.0f;
        if (totalGoodsRequired > totalGoods && totalGoodsRequired > 0) {
            goodsEfficiency = totalGoods / totalGoodsRequired;
        }

        log(String.format("--- Updating Population [goods required=%.2f] [goods available=%.2f] [efficiency=%.2f]",
                          totalGoodsRequired, totalGoods, goodsEfficiency));

        // subtract all the goods we'll need
        totalGoods -= totalGoodsRequired;
        if (totalGoods <= 0.0f) {
            // We've run out of goods! That's bad...
            totalGoods = 0.0f;

            if (empire != null) {
                if (empire.getGoodsZeroTime() == null || empire.getGoodsZeroTime().isAfter(now.plus(dt))) {
                    log(String.format("    GOODS HAVE HIT ZERO"));
                    empire.setGoodsZeroTime(now.plus(dt));
                }
            }
        }

        // now loop through the colonies and update the population/goods counter
        for (BaseColony colony : star.getColonies()) {
            if (!equalEmpireKey(colony.getEmpireKey(), empireKey)) {
                continue;
            }

            float populationIncrease;
            if (goodsEfficiency >= 1.0f) {
                populationIncrease = Math.max(colony.getPopulation(), 10.0f);
                populationIncrease *= colony.getPopulationFocus() * 0.5f;
            } else {
                populationIncrease = Math.max(colony.getPopulation(), 10.0f);
                populationIncrease *= (1.0f - colony.getPopulationFocus());
                populationIncrease *= 0.25f * (goodsEfficiency - 1.0f);
            }

            colony.setPopulationDelta(populationIncrease);
            float populationIncreaseThisTurn = populationIncrease * dtInHours;

            float newPopulation = colony.getPopulation() + populationIncreaseThisTurn;
            if (newPopulation < 1.0f) {
                newPopulation = 0.0f;
            } else if (newPopulation > colony.getMaxPopulation()) {
                newPopulation = colony.getMaxPopulation();
            }
            if (newPopulation < 100.0f && colony.isInCooldown()) {
                newPopulation = 100.0f;
            }
            log(String.format("    Colony[%d]: [delta=%.2f] [new=%.2f]",
                              colony.getPlanetIndex(), populationIncrease, newPopulation));
            colony.setPopulation(newPopulation);
        }

        if (totalGoods > maxGoods) {
            totalGoods = maxGoods;
        }
        if (totalMinerals > maxMinerals) {
            totalMinerals = maxMinerals;
        }

        if (empire != null) {
            empire.setTotalGoods(totalGoods);
            empire.setTotalMinerals(totalMinerals);
            empire.setDeltaGoodsPerHour(goodsDeltaPerHour);
            empire.setDeltaMineralsPerHour(mineralsDeltaPerHour);
            empire.setTaxPerHour(totalTaxPerHour);
        }
    }

    private void simulateCombat(BaseStar star, DateTime now, Duration dt) {
        // if there's no fleets in ATTACKING mode, then there's nothing to do
        int numAttacking = 0;
        for (BaseFleet fleet : star.getFleets()) {
            if (fleet.getState() != BaseFleet.State.ATTACKING || isDestroyed(fleet, now)) {
                continue;
            }
            numAttacking ++;
        }
        if (numAttacking == 0) {
            return;
        }

        // get the existing combat report, or create a new one
        BaseCombatReport combatReport = star.getCombatReport();
        if (combatReport == null) {
            log(String.format("-- Combat [new combat report] [%d attacking]", numAttacking));
            combatReport = star.createCombatReport(null);
            star.setCombatReport(combatReport);
        } else {
            // remove any rounds that are in the future
            for (int i = 0; i < combatReport.getCombatRounds().size(); i++) {
                BaseCombatReport.CombatRound round = combatReport.getCombatRounds().get(i);
                if (round.getRoundTime().isAfter(now)) {
                    combatReport.getCombatRounds().remove(i);
                    i--;
                }
            }

            log(String.format("-- Combat, [loaded %d rounds] [%d attacking]", combatReport.getCombatRounds().size(), numAttacking));
        }

        DateTime attackStartTime = null;
        for (BaseFleet fleet : star.getFleets()) {
            if (fleet.getState() != BaseFleet.State.ATTACKING) {
                continue;
            }
            if (attackStartTime == null || attackStartTime.isAfter(fleet.getStateStartTime())) {
                attackStartTime = fleet.getStateStartTime();
            }
        }

        if (attackStartTime.isBefore(now)) {
            attackStartTime = now;
        }

        // round up to the next minute
        attackStartTime = new DateTime(
                attackStartTime.getYear(), attackStartTime.getMonthOfYear(), attackStartTime.getDayOfMonth(),
                attackStartTime.getHourOfDay(), attackStartTime.getMinuteOfHour(), 0);
        attackStartTime = attackStartTime.plusMinutes(1);

        // if they're not supposed to start attacking yet, then don't start
        if (attackStartTime.isAfter(now.plus(dt))) {
            return;
        }

        // attacks happen in turns, each turn lasts for one minute
        DateTime attackEndTime = now.plus(dt);
        while (now.isBefore(attackEndTime)) {
            if (now.isBefore(attackStartTime)) {
                now = now.plusMinutes(1);
                if (now.isAfter(attackStartTime)) {
                    now = attackStartTime;
                }
                continue;
            }

            BaseCombatReport.CombatRound round = new BaseCombatReport.CombatRound();
            round.setStarKey(star.getKey());
            round.setRoundTime(now);
            log(String.format("--- Round #%d [%s]", combatReport.getCombatRounds().size() + 1, now));
            boolean stillAttacking = simulateCombatRound(now, star, round);
            if (combatReport.getStartTime() == null) {
                combatReport.setStartTime(now);
            }
            combatReport.setEndTime(now);
            combatReport.getCombatRounds().add(round);

            if (!stillAttacking) {
                log(String.format("--- Combat finished."));
                break;
            }
            now = now.plusMinutes(1);
        }
    }

    private boolean simulateCombatRound(DateTime now, BaseStar star, BaseCombatReport.CombatRound round) {
        for (BaseFleet fleet : star.getFleets()) {
            if (isDestroyed(fleet, now)) {
                continue;
            }
            // if it's got a cloaking device and it's not aggressive, then it's invisible to combat
            if (fleet.hasUpgrade("cloak") && fleet.getStance() != Stance.AGGRESSIVE) {
                continue;
            }

            BaseCombatReport.FleetSummary fleetSummary = new BaseCombatReport.FleetSummary(fleet);
            round.getFleets().add(fleetSummary);
        }

        // now we go through the fleet summaries and join them together
        for (int i = 0; i < round.getFleets().size(); i++) {
            BaseCombatReport.FleetSummary fs1 = round.getFleets().get(i);
            for (int j = i + 1; j < round.getFleets().size(); j++) {
                BaseCombatReport.FleetSummary fs2 = round.getFleets().get(j);

                if (!isFriendly(fs1, fs2)) {
                    continue;
                }
                if (!fs1.getDesignID().equals(fs2.getDesignID())) {
                    continue;
                }
                if (fs1.getFleetStance() != fs2.getFleetStance()) {
                    continue;
                }
                if (fs1.getFleetState() != fs2.getFleetState()) {
                    continue;
                }

                // same empire, same design, same stance/state -- join 'em!
                fs1.addShips(fs2);
                round.getFleets().remove(j);
                j--;
            }
        }

        for (int i = 0; i < round.getFleets().size(); i++) {
            BaseCombatReport.FleetSummary fleet = round.getFleets().get(i);
            fleet.setIndex(i);
        }

        // each fleet targets and fires at once
        TreeMap<Integer, Double> hits = new TreeMap<Integer, Double>();
        for (BaseCombatReport.FleetSummary fleet : round.getFleets()) {
            if (fleet.getFleetState() != BaseFleet.State.ATTACKING) {
                continue;
            }

            BaseCombatReport.FleetSummary target = findTarget(round, fleet);
            if (target == null) {
                // if there's no more available targets, then we're no longer attacking
                log(String.format("    Fleet #%d no suitable target.", fleet.getIndex()));
                fleet.setFleetState(BaseFleet.State.IDLE);
                continue;
            } else {
                log(String.format("    Fleet #%d attacking fleet #%d", fleet.getIndex(), target.getIndex()));
            }

            ShipDesign fleetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
            float damage = fleet.getNumShips() * fleetDesign.getBaseAttack();
            log(String.format("    Fleet #%d (%s x %.2f) hit by fleet #%d (%s x %.2f) for %.2f damage",
                    target.getIndex(), target.getDesignID(), target.getNumShips(),
                    fleet.getIndex(), fleet.getDesignID(), fleet.getNumShips(), damage));

            Double totalDamage = hits.get(target.getIndex());
            if (totalDamage == null) {
                hits.put(target.getIndex(), new Double(damage));
            } else {
                hits.put(target.getIndex(), new Double(totalDamage + damage));
            }

            BaseCombatReport.FleetAttackRecord attackRecord = new BaseCombatReport.FleetAttackRecord(
                    round.getFleets(), fleet.getIndex(), target.getIndex(), damage);
            round.getFleetAttackRecords().add(attackRecord);
        }

        // any fleets that were attacked this round will want to change to attacking for the next
        // round, if they're not attacking already...
        for (BaseCombatReport.FleetSummary fleet : round.getFleets()) {
            if (!hits.keySet().contains(fleet.getIndex())) {
                continue;
            }
            for (BaseFleet targetFleet : fleet.getFleets()) {
                if (targetFleet.getState() == BaseFleet.State.IDLE) {
                    ShipDesign targetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
                    ArrayList<ShipEffect> effects = targetDesign.getEffects(ShipEffect.class);
                    for (ShipEffect effect : effects) {
                        effect.onAttacked(star, targetFleet);
                    }
                }
            }

        }

        // next, apply the damage from this round
        for (BaseCombatReport.FleetSummary fleet : round.getFleets()) {
            Double damage = hits.get(fleet.getIndex());
            if (damage == null) {
                continue;
            }

            ShipDesign fleetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
            damage /= fleetDesign.getBaseDefence();
            fleet.removeShips((float) (double) damage);
            log(String.format("    Fleet #%d %.2f ships lost (%.2f ships remaining)", fleet.getIndex(), damage, fleet.getNumShips()));

            BaseCombatReport.FleetDamagedRecord damageRecord = new BaseCombatReport.FleetDamagedRecord(
                    round.getFleets(), fleet.getIndex(), (float) damage.doubleValue());
            round.getFleetDamagedRecords().add(damageRecord);

            // go through the "real" fleets and apply the damage as well
            for (String fleetKey : fleet.getFleetKeys()) {
                BaseFleet realFleet = star.findFleet(fleetKey);
                float newNumShips = (float)(realFleet.getNumShips() - damage);
                if (newNumShips <= 0) {
                    newNumShips = 0;
                }
                realFleet.setNumShips(newNumShips);
                if (realFleet.getNumShips() <= 0.0f) {
                    realFleet.setTimeDestroyed(now);
                }

                if (damage <= 0) {
                    break;
                }
            }
        }

        // if all the fleets are friendly (or running away), we can stop attacking
        boolean enemyExists = false;
        for (int i = 0; i < star.getFleets().size(); i++) {
            BaseFleet fleet1 = star.getFleets().get(i);
            if (isDestroyed(fleet1, now) || fleet1.getState() == BaseFleet.State.MOVING) {
                continue;
            }

            for (int j = i + 1; j < star.getFleets().size(); j++) {
                BaseFleet fleet2 = star.getFleets().get(j);
                if (isDestroyed(fleet2, now)) {
                    continue;
                }

                if (!isFriendly(fleet1, fleet2)) {
                    if (fleet2.getState() == BaseFleet.State.MOVING) {
                        // if it's moving, it doesn't count
                        continue;
                    }
                    enemyExists = true;
                }
            }
        }
        if (!enemyExists) {
            for (BaseFleet fleet : star.getFleets()) {
                // switch back from attacking mode to idle
                if (fleet.getState() == BaseFleet.State.ATTACKING) {
                    fleet.idle(now);
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Searches for an enemy fleet with the lowest priority.
     */
    private BaseCombatReport.FleetSummary findTarget(BaseCombatReport.CombatRound round,
                                                     BaseCombatReport.FleetSummary fleet) {
        int foundPriority = 9999;
        BaseCombatReport.FleetSummary foundFleet = null;

        for (BaseCombatReport.FleetSummary otherFleet : round.getFleets()) {
            if (isFriendly(fleet, otherFleet)) {
                continue;
            }
            if (otherFleet.getFleetState() == BaseFleet.State.MOVING) {
                continue;
            }
            ShipDesign design = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, otherFleet.getDesignID());
            if (foundFleet == null || design.getCombatPriority() < foundPriority) {
                foundFleet = otherFleet;
                foundPriority = design.getCombatPriority();
            }
        }

        return foundFleet;
    }

    private static boolean isFriendly(BaseCombatReport.FleetSummary fleet1, BaseCombatReport.FleetSummary fleet2) {
        BaseFleet baseFleet1 = fleet1.getFleets().get(0);
        BaseFleet baseFleet2 = fleet2.getFleets().get(0);
        return isFriendly(baseFleet1, baseFleet2);
    }

    public static boolean isFriendly(BaseFleet fleet1, BaseFleet fleet2) {
        if (fleet1.getEmpireKey() == null && fleet2.getEmpireKey() == null) {
            // if they're both native (i.e. no empire key) they they're friendly
            return true;
        }
        if (fleet1.getEmpireKey() == null || fleet2.getEmpireKey() == null) {
            // if one is native and one is non-native, then they're not friendly
            return false;
        }
        if (fleet1.getEmpireKey().equals(fleet2.getEmpireKey())) {
            // if they're both the same empire they're friendly
            return true;
        }

        // check whether they're the same alliance, in which case they're friendly
        if (fleet1.getAllianceID() != null && fleet2.getAllianceID() != null &&
                fleet1.getAllianceID() == fleet2.getAllianceID()) {
            return true;
        }

        // otherwise they're enemies
        return false;
    }

    private boolean isDestroyed(BaseFleet fleet, DateTime now) {
        if (fleet.getTimeDestroyed() != null && !fleet.getTimeDestroyed().isAfter(now)) {
            return true;
        }
        return false;
    }

    /**
     * This interface is used to help debug the simulation code. Implement it to receive a bunch
     * of debug log messages during the simulation process.
     */
    public interface LogHandler {
        void log(String message);
    }

    private static class BasicLogHandler implements LogHandler {
        private static final Log log = new Log("Simulation");

        @Override
        public void log(String message) {
            log.info(message);
        }
    }
}
