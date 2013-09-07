package au.com.codeka.common.sim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Seconds;

import au.com.codeka.common.design.BaseDesignManager;
import au.com.codeka.common.design.Design;
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.design.ShipDesign;
import au.com.codeka.common.design.ShipEffect;
import au.com.codeka.common.model.BuildRequest;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.CombatReport;
import au.com.codeka.common.model.CombatRound;
import au.com.codeka.common.model.EmpirePresence;
import au.com.codeka.common.model.Fleet;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Planet;
import au.com.codeka.common.model.Star;


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
    public void simulate(Star star) {
        log(String.format("Begin simulation for '%s'", star.name));

        HashSet<String> empireKeys = new HashSet<String>();
        for (Colony colony : star.colonies) {
            if (!empireKeys.contains(colony.empire_key)) {
                empireKeys.add(colony.empire_key);
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
        Star predictionStar = null;

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

                    try {
                        predictionStar = Model.wire.parseFrom(star.toByteArray(), Star.class);
                    } catch (IOException e) {
                    }
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
            for (BuildRequest starBuildRequest : star.build_requests) {
                for (BuildRequest predictedBuildRequest : predictionStar.build_requests) {
                    if (starBuildRequest.key.equals(predictedBuildRequest.key)) {
                        starBuildRequest.end_time = predictedBuildRequest.end_time;
                    }
                }
            }

            // any fleets that *will be* destroyed, remember the time of their death
            for (Fleet fleet : star.fleets) {
                for (Fleet predictedFleet : predictionStar.fleets) {
                    if (fleet.key.equals(predictedFleet.key)) {
                        log(String.format("Fleet #%s updating timeDestroyed to: %s", fleet.key,
                                Model.toDateTime(predictedFleet.time_destroyed)));
                        fleet.time_destroyed = predictedFleet.time_destroyed;
                    }
                }
            }

            // if the empire is going to run out of resources, save that time as well.
            for (EmpirePresence empirePresence : star.empires) {
                for (EmpirePresence predictedEmpirePresence : predictionStar.empires) {
                    if (empirePresence.key.equals(predictedEmpirePresence.key)) {
                        empirePresence.goods_zero_time = predictedEmpirePresence.goods_zero_time;
                    }
                }
            }

            // also, the prediction combat report (if any) is the one to use
            star.current_combat_report = predictionStar.current_combat_report;

            star.last_simulation = Model.fromDateTime(endTime);
        }
    }

    private DateTime getSimulateStartTime(Star star) {
        DateTime lastSimulation = Model.toDateTime(star.last_simulation);
        if (lastSimulation == null) {
            for (Fleet fleet : star.fleets) {
                DateTime dt = Model.toDateTime(fleet.state_start_time);
                if (lastSimulation == null || dt.compareTo(lastSimulation) < 0) {
                    lastSimulation = dt;
                }
            }
        }
        return lastSimulation;
    }

    private void simulateStepForAllEmpires(Duration dt, DateTime now, Star star, Set<String> empireKeys) {
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

    private void simulateStep(Duration dt, DateTime now, Star star, String empireKey) {
        float totalGoods = 50.0f;
        float totalMinerals = 50.0f;
        float totalPopulation = 0.0f;
        float maxGoods = 50.0f;
        float maxMinerals = 50.0f;

        EmpirePresence empire = null;
        for (EmpirePresence e : star.empires) {
            if (!equalEmpireKey(e.empire_key, empireKey)) {
                continue;
            }
            empire = e;
            totalGoods = empire.total_goods;
            totalMinerals = empire.total_minerals;
            maxGoods = empire.max_goods;
            maxMinerals = empire.max_minerals;
        }

        float dtInHours = ((float) dt.getMillis()) / (1000.0f * 3600.0f);
        float goodsDeltaPerHour = 0.0f;
        float mineralsDeltaPerHour = 0.0f;

        for (Colony colony : star.colonies) {
            if (!equalEmpireKey(colony.empire_key, empireKey)) {
                continue;
            }

            log(String.format("--- Colony [planetIndex=%d] [population=%.2f]",
                    colony.planet_index, colony.population));
            Planet planet = star.planets.get(colony.planet_index - 1);

            // calculate the output from farming this turn and add it to the star global
            float goods = colony.population * colony.focus_farming *
                          (planet.farming_congeniality / 100.0f);
            colony.delta_goods = goods;
            totalGoods += goods * dtInHours;
            goodsDeltaPerHour += goods;
            log(String.format("    Goods: [delta=%.2f / hr] [this turn=%.2f]", goods, goods * dtInHours));

            // calculate the output from mining this turn and add it to the star global
            float minerals = colony.population * colony.focus_mining *
                             (planet.mining_congeniality / 100.0f);
            colony.delta_minerals = minerals;
            totalMinerals += minerals * dtInHours;
            mineralsDeltaPerHour += minerals;
            log(String.format("    Minerals: [delta=%.2f / hr] [this turn=%.2f]", goods, goods * dtInHours));

            totalPopulation += colony.population;

            // work out the amount of taxes this colony has generated in the last turn
            float taxPerPopulationPerHour = 0.012f;
            float taxThisTurn = taxPerPopulationPerHour * dtInHours * colony.population;
            log(String.format("    Taxes %.2f + %.2f = %.2f uncollected", colony.uncollected_taxes, taxThisTurn,
                    colony.uncollected_taxes + taxThisTurn));
            colony.uncollected_taxes += taxThisTurn;
        }

        // A second loop though the colonies, once the goods/minerals have been calculated. This way,
        // goods minerals are shared between colonies
        for (Colony colony : star.colonies) {
            if (!equalEmpireKey(colony.empire_key, empireKey)) {
                continue;
            }

            ArrayList<BuildRequest> buildRequests = new ArrayList<BuildRequest>();
            for (BuildRequest br : star.build_requests) {
                if (br.colony_key.equals(colony.key)) {
                    buildRequests.add(br);
                }
            }

            // not all build requests will be processed this turn. We divide up the population
            // based on the number of ACTUAL build requests they'll be working on this turn
            int numValidBuildRequests = 0;
            for (BuildRequest br : buildRequests) {
                if (Model.toDateTime(br.start_time).compareTo(now.plus(dt)) > 0) {
                    continue;
                }

                // the end_time will be accurate, since it'll have been updated last step
                DateTime endTime = Model.toDateTime(br.end_time);
                if (endTime.compareTo(now) < 0 && endTime.compareTo(year2k) > 0) {
                    continue;
                }

                // as long as it's started but hasn't finished, we'll be working on it this turn
                numValidBuildRequests += 1;
            }

            // If we have pending build requests, we'll have to update them as well
            if (numValidBuildRequests > 0) {
                float totalWorkers = colony.population * colony.focus_construction;
                float workersPerBuildRequest = totalWorkers / numValidBuildRequests;
                log(String.format("--- Building [buildRequests=%d] [planetIndex=%d] [totalWorker=%.2f]",
                        numValidBuildRequests, colony.planet_index, totalWorkers));

                // OK, we can spare at least ONE population
                if (workersPerBuildRequest < 1.0f) {
                    workersPerBuildRequest = 1.0f;
                }

                // divide the minerals up per build request, so they each get a share. I'm not sure
                // if we should portion minerals out by how 'big' the build request is, but we'll
                // see how this goes initially
                float mineralsPerBuildRequest = totalMinerals / numValidBuildRequests;

                for (BuildRequest br : buildRequests) {
                    Design design = BaseDesignManager.i.getDesign(DesignKind.fromBuildKind(br.build_kind), br.design_id);
                    log(String.format("---- Building [design=%s %s] [count=%d]",
                            br.build_kind, br.design_id, br.count));

                    DateTime startTime = Model.toDateTime(br.start_time);
                    if (startTime.compareTo(now.plus(dt)) > 0) {
                        continue;
                    }

                    // So the build time the design specifies is the time to build the structure with
                    // 100 workers available. Double the workers and you halve the build time. Halve
                    // the workers and you double the build time.
                    float totalBuildTimeInHours = br.count * design.getBuildCost().getTimeInSeconds() / 3600.0f;
                    totalBuildTimeInHours *= (100.0 / workersPerBuildRequest);

                    // the number of hours of work required, assuming we have all the minerals we need
                    float timeRemainingInHours = (1.0f - br.progress) * totalBuildTimeInHours;
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
                        timeRemainingInHours = (1.0f - br.progress) * totalBuildTimeInHours;
                        if (timeRemainingInHours < (10.0f / 3600.0f)) {
                            endTime = now;
                        } else {
                            endTime = now.plus((long)(timeRemainingInHours * 3600.0f * 1000.0f));
                        }
                        if (Model.toDateTime(br.end_time).isBefore(endTime)) {
                            br.end_time = Model.fromDateTime(endTime);
                        }
                        continue;
                    }

                    // work out how many minerals we require for this turn
                    float mineralsRequired = br.count * design.getBuildCost().getCostInMinerals() * progressThisTurn;
                    if (mineralsRequired > mineralsPerBuildRequest) {
                        // if we don't have enough minerals, we'll just do a percentage of the work
                        // this turn
                        totalMinerals -= mineralsPerBuildRequest;
                        float percentMineralsAvailable = mineralsPerBuildRequest / mineralsRequired;
                        br.progress += progressThisTurn * percentMineralsAvailable;
                        log(String.format("     Progress %.4f%% + %.4f%% (this turn, adjusted - %.4f%% originally) ",
                            br.progress * 100.0f,
                            progressThisTurn * percentMineralsAvailable * 100.0f,
                            progressThisTurn * 100.0f));
                    } else {
                        // awesome, we have enough minerals so we can make some progress. We'll start by
                        // removing the minerals we need from the global pool...
                        totalMinerals -= mineralsRequired;
                        br.progress += progressThisTurn;
                        log(String.format("     Progress %.4f%% + %.4f%% (this turn)",
                            br.progress * 100.0f, progressThisTurn * 100.0f));
                    }
                    mineralsDeltaPerHour -= mineralsRequired / dtInHours;
                    log(String.format("     Minerals [required=%.2f] [available=%.2f] [available per build=%.2f]",
                            mineralsRequired, totalMinerals, mineralsPerBuildRequest));

                    // adjust the end_time for this turn
                    timeRemainingInHours = (1.0f - br.progress) * totalBuildTimeInHours;
                    DateTime endTime = now.plus((long)(dtUsed * 1000 * 3600) + (long)(timeRemainingInHours * 1000 * 3600));
                    br.end_time = Model.fromDateTime(endTime);
                    log(String.format("     End Time: %s (%.2f hrs)", endTime, Seconds.secondsBetween(now, endTime).getSeconds() / 3600.0f));

                    if (br.progress >= 1.0f) {
                        // if we've finished this turn, just set progress
                        br.progress = 1.0f;
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
                if (empire.goods_zero_time == null || Model.toDateTime(empire.goods_zero_time).isAfter(now.plus(dt))) {
                    log(String.format("    GOODS HAVE HIT ZERO"));
                    empire.goods_zero_time = Model.fromDateTime(now.plus(dt));
                }
            }
        }

        // now loop through the colonies and update the population/goods counter
        for (Colony colony : star.colonies) {
            if (!equalEmpireKey(colony.empire_key, empireKey)) {
                continue;
            }

            float populationIncrease;
            if (goodsEfficiency >= 1.0f) {
                populationIncrease = Math.max(colony.population, 10.0f);
                populationIncrease *= colony.focus_population * 0.5f;
            } else {
                populationIncrease = Math.max(colony.population, 10.0f);
                populationIncrease *= (1.0f - colony.focus_population);
                populationIncrease *= 0.25f * (goodsEfficiency - 1.0f);
            }

            colony.delta_population = populationIncrease;
            float populationIncreaseThisTurn = populationIncrease * dtInHours;

            float newPopulation = colony.population + populationIncreaseThisTurn;
            if (newPopulation < 1.0f) {
                newPopulation = 0.0f;
            } else if (newPopulation > colony.max_population) {
                newPopulation = colony.max_population;
            }
            boolean isInCooldown = (colony.cooldown_end_time != null && Model.toDateTime(colony.cooldown_end_time).isAfter(now));
            if (newPopulation < 100.0f && isInCooldown) {
                newPopulation = 100.0f;
            }
            log(String.format("    Colony[%d]: [delta=%.2f] [new=%.2f]",
                              colony.planet_index, populationIncrease, newPopulation));
            colony.population = newPopulation;
        }

        if (totalGoods > maxGoods) {
            totalGoods = maxGoods;
        }
        if (totalMinerals > maxMinerals) {
            totalMinerals = maxMinerals;
        }

        if (empire != null) {
            empire.total_goods = totalGoods;
            empire.total_minerals = totalMinerals;
            empire.goods_delta_per_hour = goodsDeltaPerHour;
            empire.minerals_delta_per_hour = mineralsDeltaPerHour;
        }
    }

    private void simulateCombat(Star star, DateTime now, Duration dt) {
        // if there's no fleets in ATTACKING mode, then there's nothing to do
        int numAttacking = 0;
        for (Fleet fleet : star.fleets) {
            if (fleet.state != Fleet.FLEET_STATE.ATTACKING || isDestroyed(fleet, now)) {
                continue;
            }
            numAttacking ++;
        }
        if (numAttacking == 0) {
            return;
        }

        // get the existing combat report, or create a new one
        CombatReport combatReport = star.current_combat_report;
        if (combatReport == null) {
            log(String.format("-- Combat [new combat report] [%d attacking]", numAttacking));
            combatReport = new CombatReport.Builder().build();
            star.current_combat_report = combatReport;
        } else {
            // remove any rounds that are in the future
            for (int i = 0; i < combatReport.rounds.size(); i++) {
                CombatRound round = combatReport.rounds.get(i);
                if (Model.toDateTime(round.round_time).isAfter(now)) {
                    combatReport.rounds.remove(i);
                    i--;
                }
            }

            log(String.format("-- Combat, [loaded %d rounds] [%d attacking]", combatReport.rounds.size(), numAttacking));
        }

        DateTime attackStartTime = null;
        for (Fleet fleet : star.fleets) {
            if (fleet.state != Fleet.FLEET_STATE.ATTACKING) {
                continue;
            }
            if (attackStartTime == null || attackStartTime.isAfter(Model.toDateTime(fleet.state_start_time))) {
                attackStartTime = Model.toDateTime(fleet.state_start_time);
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

            CombatRound round = new CombatRound.Builder()
                .star_key(star.key)
                .round_time(Model.fromDateTime(now))
                .build();
            log(String.format("--- Round #%d [%s]", combatReport.rounds.size() + 1, now));
            boolean stillAttacking = simulateCombatRound(now, star, round);
            if (combatReport.start_time == null) {
                combatReport.start_time = Model.fromDateTime(now);
            }
            combatReport.end_time = Model.fromDateTime(now);
            combatReport.rounds.add(round);

            if (!stillAttacking) {
                log(String.format("--- Combat finished."));
                break;
            }
            now = now.plusMinutes(1);
        }
    }

    private boolean simulateCombatRound(DateTime now, Star star, CombatRound round) {
        for (Fleet fleet : star.fleets) {
            if (isDestroyed(fleet, now)) {
                continue;
            }

            CombatRound.FleetSummary fleetSummary = new CombatRound.FleetSummary.Builder()
                .design_id(fleet.design_id)
                .empire_key(fleet.empire_key)
                .num_ships(fleet.num_ships)
                .state(fleet.state)
                .stance(fleet.stance)
                .fleet_keys(Arrays.asList(fleet.key)).build();
            round.fleets.add(fleetSummary);
        }

        // now we go through the fleet summaries and join them together
        for (int i = 0; i < round.fleets.size(); i++) {
            CombatRound.FleetSummary fs1 = round.fleets.get(i);
            for (int j = i + 1; j < round.fleets.size(); j++) {
                CombatRound.FleetSummary fs2 = round.fleets.get(j);

                if (!isSameEmpire(fs1.empire_key, fs2.empire_key)) {
                    continue;
                }
                if (!fs1.design_id.equals(fs2.design_id)) {
                    continue;
                }
                if (fs1.state != fs2.state) {
                    continue;
                }
                if (fs1.stance != fs2.stance) {
                    continue;
                }

                // same empire, same design, same stance/state -- join 'em!
                fs1.num_ships += fs2.num_ships;
                round.fleets.remove(j);
                j--;
            }
        }

        HashMap<CombatRound.FleetSummary, Integer> indexMap = new HashMap<CombatRound.FleetSummary, Integer>();
        for (int i = 0; i < round.fleets.size(); i++) {
            CombatRound.FleetSummary fleet = round.fleets.get(i);
            indexMap.put(fleet, i);
        }

        // each fleet targets and fires at once
        HashMap<CombatRound.FleetSummary, Double> hits = new HashMap<CombatRound.FleetSummary, Double>();
        for (CombatRound.FleetSummary fleet : round.fleets) {
            if (fleet.state != Fleet.FLEET_STATE.ATTACKING) {
                continue;
            }

            CombatRound.FleetSummary target = findTarget(round, fleet);
            if (target == null) {
                // if there's no more available targets, then we're no longer attacking
                log(String.format("    Fleet #%d no suitable target.", indexMap.get(fleet)));
                continue;
            } else {
                log(String.format("    Fleet #%d attacking fleet #%d", indexMap.get(fleet), indexMap.get(target)));
            }

            ShipDesign fleetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
            float damage = fleet.num_ships * fleetDesign.getBaseAttack();
            log(String.format("    Fleet #%d (%s x %.2f) hit by fleet #%d (%s x %.2f) for %.2f damage",
                    indexMap.get(target), target.design_id, target.num_ships,
                    indexMap.get(fleet), fleet.design_id, fleet.num_ships, damage));

            Double totalDamage = hits.get(target);
            if (totalDamage == null) {
                hits.put(target, new Double(damage));
            } else {
                hits.put(target, new Double(totalDamage + damage));
            }

            CombatRound.FleetAttackRecord attackRecord = new CombatRound.FleetAttackRecord.Builder()
                .fleet_index(indexMap.get(fleet))
                .damage(damage)
                .target_index(indexMap.get(target))
                .build();
            round.fleets_attacked.add(attackRecord);
        }

        // any fleets that were attacked this round will want to change to attacking for the next
        // round, if they're not attacking already...
        for (CombatRound.FleetSummary fleet : round.fleets) {
            if (!hits.containsKey(fleet)) {
                continue;
            }
            for (Fleet targetFleet : star.fleets) {
                boolean isTargetted = false;
                for (String fleetKey : fleet.fleet_keys) {
                    if (fleetKey.equals(targetFleet.key)){
                        isTargetted = true;
                    }
                }
                if (isTargetted && targetFleet.state == Fleet.FLEET_STATE.IDLE) {
                    ShipDesign targetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
                    ArrayList<ShipEffect> effects = targetDesign.getEffects(ShipEffect.class);
                    for (ShipEffect effect : effects) {
                        effect.onAttacked(star, targetFleet);
                    }
                }
            }

        }

        // next, apply the damage from this round
        for (CombatRound.FleetSummary fleet : round.fleets) {
            Double damage = hits.get(fleet);
            if (damage == null) {
                continue;
            }

            ShipDesign fleetDesign = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
            damage /= fleetDesign.getBaseDefence();
            fleet.num_ships = Math.max(0, fleet.num_ships - (float) damage.doubleValue());
            log(String.format("    Fleet #%d %.2f ships lost (%.2f ships remaining)", indexMap.get(fleet), damage, fleet.num_ships));

            CombatRound.FleetDamagedRecord damageRecord = new CombatRound.FleetDamagedRecord.Builder()
                .fleet_index(indexMap.get(fleet))
                .damage((float) (double) damage)
                .build();
            round.fleets_damaged.add(damageRecord);

            // go through the "real" fleets and apply the damage as well
            for (String fleetKey : fleet.fleet_keys) {
                Fleet realFleet = Model.findFleet(star, fleetKey);
                float newNumShips = (float)(realFleet.num_ships - damage);
                if (newNumShips <= 0) {
                    newNumShips = 0;
                }
                realFleet.num_ships = newNumShips;
                if (realFleet.num_ships <= 0.0f) {
                    realFleet.time_destroyed = Model.fromDateTime(now);
                }

                if (damage <= 0) {
                    break;
                }
            }
        }

        // if all the fleets are friendly, we can stop attacking
        boolean enemyExists = false;
        for (int i = 0; i < star.fleets.size(); i++) {
            Fleet fleet1 = star.fleets.get(i);
            if (isDestroyed(fleet1, now)) {
                continue;
            }

            for (int j = i + 1; j < star.fleets.size(); j++) {
                Fleet fleet2 = star.fleets.get(j);
                if (isDestroyed(fleet2, now)) {
                    continue;
                }

                if (!isSameEmpire(fleet1.empire_key, fleet2.empire_key)) {
                    enemyExists = true;
                }
            }
        }
        if (!enemyExists) {
            for (Fleet fleet : star.fleets) {
                Model.idle(fleet, now);
            }
            return false;
        }
        return true;
    }

    /**
     * Searches for an enemy fleet with the lowest priority.
     */
    private CombatRound.FleetSummary findTarget(CombatRound round,
                                                CombatRound.FleetSummary fleet) {
        int foundPriority = 9999;
        CombatRound.FleetSummary foundFleet = null;

        for (CombatRound.FleetSummary otherFleet : round.fleets) {
            if (isSameEmpire(fleet.empire_key, otherFleet.empire_key)) {
                continue;
            }
            if (otherFleet.state == Fleet.FLEET_STATE.MOVING) {
                continue;
            }
            ShipDesign design = (ShipDesign) BaseDesignManager.i.getDesign(DesignKind.SHIP, otherFleet.design_id);
            if (foundFleet == null || design.getCombatPriority() < foundPriority) {
                foundFleet = otherFleet;
                foundPriority = design.getCombatPriority();
            }
        }

        return foundFleet;
    }

    private static boolean isSameEmpire(String key1, String key2) {
        if (key1 == null && key2 == null) {
            return true;
        }
        if (key1 == null || key2 == null) {
            return false;
        }
        return key1.equals(key2);
    }

    private boolean isDestroyed(Fleet fleet, DateTime now) {
        if (fleet.time_destroyed == null) {
            return false;
        }
        if (Model.toDateTime(fleet.time_destroyed).isAfter(now)) {
            return false;
        }
        return true;
    }

    /**
     * This interface is used to help debug the simulation code. Implement it to receive a bunch
     * of debug log messages during the simulation process.
     */
    public interface LogHandler {
        void log(String message);
    }
}
