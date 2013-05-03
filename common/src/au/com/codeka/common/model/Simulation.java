package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;

/**
 * This class is used to simulate a \c Star. It need to have the same logic as ctrl/simulation.py
 * on the server, and we go to great pains to keep them in sync.
 */
public class Simulation {
    private LogHandler mLogHandler;
    private DateTime mNow;

    private static DateTime year2k = new DateTime(2000, 1, 1, 0, 0);

    public Simulation() {
        mNow = DateTime.now(DateTimeZone.UTC);
    }
    public Simulation(LogHandler log) {
        mNow = DateTime.now(DateTimeZone.UTC);
        mLogHandler = log;
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
        log(String.format("Begin simulation for '%s'", star.getName()));

        HashSet<String> empireKeys = new HashSet<String>();
        for (BaseColony colony : star.getColonies()) {
            if (!empireKeys.contains(colony.getEmpireKey())) {
                empireKeys.add(colony.getEmpireKey());
            }
        }

        // if there's any missing EmpirePresence objects, add them now

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

            // also, the prediction combat report (if any) is the one to use
            star.setCombatReport(predictionStar.getCombatReport());

            star.setLastSimulation(mNow);
        }

        //log.debug(String.format("End simulation for '%s'", star.getName()));
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

        for (BaseEmpirePresence empire : star.getEmpires()) {
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

                for (BaseBuildRequest br : buildRequests) {
                    Design design = BaseDesignManager.i.getDesign(br.getDesignKind(), br.getDesignID());
                    log(String.format("---- Building [design=%s %s] [count=%d]",
                            br.getDesignKind(), br.getDesignID(), br.getCount()));

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
                        timeRemainingInHours = (1.0f - br.getProgress(false)) * totalBuildTimeInHours;
                        DateTime endTime = now.plus((long)(timeRemainingInHours * 3600.0f * 1000.0f));
                        if (br.getEndTime().compareTo(endTime) > 0) {
                            br.setEndTime(endTime);
                        }
                        continue;
                    }
                    log(String.format("     Progress %.2f%% + %.2f%% (this turn)",
                            br.getProgress(false), progressThisTurn));

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
                    log(String.format("     Minerals [required=%.2f] [available=%.2f]",
                            mineralsRequired, totalMinerals));

                    // adjust the end_time for this turn
                    timeRemainingInHours = (1.0f - br.getProgress(false)) * totalBuildTimeInHours;
                    DateTime endTime = now.plus((long)(dtUsed * 1000 * 3600) + (long)(timeRemainingInHours * 1000 * 3600));
                    br.setEndTime(endTime);
                    log(String.format("     End Time: %s", endTime));

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
        for (BaseColony colony : star.getColonies()) {
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

        for (BaseEmpirePresence empire : star.getEmpires()) {
            if (!equalEmpireKey(empire.getEmpireKey(), empireKey)) {
                continue;
            }
            empire.setTotalGoods(totalGoods);
            empire.setTotalMinerals(totalMinerals);
            empire.setDeltaGoodsPerHour(goodsDeltaPerHour);
            empire.setDeltaMineralsPerHour(mineralsDeltaPerHour);
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
            combatReport = star.createCombatReport(null);
            star.setCombatReport(combatReport);
        }

        log(String.format("-- Combat (%d fleets attacking)", numAttacking));
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
                break;
            }
            now = now.plusMinutes(1);
        }
    }

    private boolean simulateCombatRound(DateTime now, BaseStar star, BaseCombatReport.CombatRound round) {
        TreeMap<String, Integer> fleetIndices = new TreeMap<String, Integer>();
        for (BaseFleet fleet : star.getFleets()) {
            if (fleet.getState() != BaseFleet.State.ATTACKING || isDestroyed(fleet, now)) {
                continue;
            }

            BaseCombatReport.FleetSummary fleetSummary = new BaseCombatReport.FleetSummary(fleet);
            round.getFleets().add(fleetSummary);
            int fleetIndex = round.getFleets().size() - 1;
            fleetIndices.put(fleet.getKey(), fleetIndex);

            // if its stateStartTime is less than now, but more than a minute ago then it's just
            // joined the fray this round.
            if (fleet.getStateStartTime().isBefore(now) && fleet.getStateStartTime().isAfter(now.minusMinutes(1))) {
                round.getFleetJoinedRecords().add(new BaseCombatReport.FleetJoinedRecord(
                        round.getFleets(), fleetIndex));
            }
        }

        // look for fleets whose targets have been destroyed and un-target them
        for (BaseFleet fleet : star.getFleets()) {
            if (fleet.getState() != BaseFleet.State.ATTACKING || isDestroyed(fleet, now)) {
                continue;
            }
            String targetFleetKey = fleet.getTargetFleetKey();
            if (targetFleetKey == null) {
                continue;
            }

            BaseFleet targetFleet = star.findFleet(targetFleetKey);
            if (targetFleet == null) {
                fleet.setTargetFleetKey(null);
            } else if (isDestroyed(targetFleet, now)) {
                log(String.format("    Fleet #%s target destroyed, finding new target", fleet.getKey()));
                fleet.setTargetFleetKey(null);
            }
        }

        // look for fleets that are ATTACKING but are not currently targetting anything
        for (BaseFleet fleet : star.getFleets()) {
            if (fleet.getState() != BaseFleet.State.ATTACKING || isDestroyed(fleet, now)) {
                continue;
            }
            String targetFleetKey = fleet.getTargetFleetKey();
            if (targetFleetKey != null) {
                continue;
            }

            BaseFleet targetFleet = findTarget(star, fleet, now);
            if (targetFleet == null) {
                // if there's no more available targets, then we're no longer attacking
                log(String.format("    Fleet #%s no suitable target, switching to idle.", fleet.getKey()));
                fleet.setState(BaseFleet.State.IDLE, now);
            } else {
                log(String.format("    Fleet #%s targetting fleet #%s", fleet.getKey(), targetFleet.getKey()));
                fleet.setTargetFleetKey(targetFleet.getKey());

                BaseCombatReport.FleetTargetRecord fleetTargetted = new BaseCombatReport.FleetTargetRecord(
                        round.getFleets(), fleetIndices.get(fleet.getKey()),
                        fleetIndices.get(targetFleet.getKey()));
                round.getFleetTargetRecords().add(fleetTargetted);
            }
        }

        // all combatting fleets fire at once...
        TreeMap<String, Double> hits = new TreeMap<String, Double>();
        for (BaseFleet fleet : star.getFleets()) {
            if (fleet.getState() != BaseFleet.State.ATTACKING || isDestroyed(fleet, now)) {
                continue;
            }
            if (fleet.getStateStartTime().isAfter(now)) {
                continue;
            }

            BaseFleet target = star.findFleet(fleet.getTargetFleetKey());
            if (target == null) {
                continue;
            }

            ShipDesign fleetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
            float damage = fleet.getNumShips() * fleetDesign.getBaseAttack();
            log(String.format("    Fleet #%s (%s x %.2f) hit by fleet %s (%s x %.2f) for %.2f damage",
                    target.getKey(), target.getDesignID(), target.getNumShips(),
                    fleet.getKey(), fleet.getDesignID(), fleet.getNumShips(), damage));

            Double totalDamage = hits.get(target.getKey());
            if (totalDamage == null) {
                hits.put(target.getKey(), new Double(damage));
            } else {
                hits.put(target.getKey(), new Double(totalDamage + damage));
            }

            BaseCombatReport.FleetAttackRecord attackRecord = new BaseCombatReport.FleetAttackRecord(
                    round.getFleets(), fleetIndices.get(fleet.getKey()), fleetIndices.get(target.getKey()), damage);
            round.getFleetAttackRecords().add(attackRecord);

            if (target.getState() == BaseFleet.State.IDLE) {
                ShipDesign targetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, target.getDesignID());
                ArrayList<ShipEffect> effects = targetDesign.getEffects(ShipEffect.class);
                for (ShipEffect effect : effects) {
                    effect.onAttacked(star, target, fleet);
                }
            }
        }

        // next, apply the damage from this round
        for (BaseFleet fleet : star.getFleets()) {
            Double damage = hits.get(fleet.getKey());
            if (damage == null) {
                continue;
            }

            ShipDesign fleetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
            damage /= fleetDesign.getBaseDefence();
            float newNumShips = (float) (fleet.getNumShips() - damage);
            if (newNumShips < 0.0f) {
                newNumShips = 0.0f;
            }
            fleet.setNumShips(newNumShips);

            BaseCombatReport.FleetDamagedRecord damageRecord = new BaseCombatReport.FleetDamagedRecord(
                    round.getFleets(), fleetIndices.get(fleet.getKey()), (float) damage.doubleValue());
            round.getFleetDamagedRecords().add(damageRecord);

            // if it's destroyed, mark it as such
            if (newNumShips == 0.0f) {
                log(String.format("    Fleet #%s DESTROYED", fleet.getKey()));
                fleet.setTimeDestroyed(now);
            }
        }

        // if there's any fleets still attacking then we need to keep going
        for (BaseFleet fleet : star.getFleets()) {
            if (fleet.getState() == BaseFleet.State.ATTACKING && !isDestroyed(fleet, now)) {
                return true;
            }
        }

        // TODO: victorious?
        return false;
    }

    private boolean isDestroyed(BaseFleet fleet, DateTime now) {
        if (fleet.getTimeDestroyed() != null && !fleet.getTimeDestroyed().isAfter(now)) {
            return true;
        }
        return false;
    }

    private BaseFleet findTarget(BaseStar star, BaseFleet fleet, DateTime now) {
        BaseFleet currentTarget = null;
        ShipDesign currentTargetDesign = null;
        for (BaseFleet targetFleet : star.getFleets()) {
            if (targetFleet.getState() == BaseFleet.State.MOVING) {
                // if this target is moving, it's not a potential taret
                continue;
            }
            if ((targetFleet.getEmpireKey() == null && fleet.getEmpireKey() == null) ||
                (targetFleet.getEmpireKey() != null && fleet.getEmpireKey() != null && targetFleet.getEmpireKey().equals(fleet.getEmpireKey()))) {
                // if this target belongs to the same empire, it's not a potential
                // TODO: alliances
                continue;
            }
            if (isDestroyed(targetFleet, now)) {
                continue;
            }

            // choose this target if it's attack capability is higher than the existing target
            ShipDesign thisDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, targetFleet.getDesignID());
            if (currentTarget == null ||
                    (currentTargetDesign.getBaseAttack() < thisDesign.getBaseAttack())) {
                currentTarget = targetFleet;
                currentTargetDesign = thisDesign;
            }
        }

        return currentTarget;
    }

    /**
     * This interface is used to help debug the simulation code. Implement it to receive a bunch
     * of debug log messages during the simulation process.
     */
    public interface LogHandler {
        void log(String message);
    }
}
