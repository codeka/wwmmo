package au.com.codeka.warworlds.common.sim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/** This class is used to simulate a {@link Star}. */
public class Simulation {
  private final LogHandler logHandler;
  private final boolean predict;
  private long timeOverride;

  private static final boolean sDebug = false;

  /** Step time is 10 minutes. */
  private static final long STEP_TIME = 10 * Time.MINUTE;

  public Simulation() {
    this(System.currentTimeMillis(), true, sDebug ? new BasicLogHandler() : null);
  }

  public Simulation(LogHandler log) {
    this(System.currentTimeMillis(), true, log);
  }

  public Simulation(boolean predict) {
    this(System.currentTimeMillis(), predict, sDebug ? new BasicLogHandler() : null);
  }

  public Simulation(long timeOverride, LogHandler log) {
    this(timeOverride, true, log);
  }

  public Simulation(long timeOverride, boolean predict, LogHandler logHandler) {
    this.timeOverride = timeOverride;
    this.predict = predict;
    this.logHandler = logHandler;
  }

  /**
   * Simulate the given star, and make sure it's "current".
   *
   * @param star The {@link Star.Builder} of the star to simulate. We modify the builder in-place
   *     with the new values.
   */
  @Nullable
  public void simulate(Star.Builder star) {
    if (logHandler != null) {
      logHandler.setStarName(star.name);
    }
    log(String.format("Begin simulation for '%s'", star.name));

    // figure out the start time, which is the oldest last_simulation time
    long startTime = getSimulateStartTime(star);
    if (startTime >= trimTimeToStep(timeOverride) && !predict) {
      log("Simulation already up-to-date, not simulating.");
      return;
    }
    long endTime = trimTimeToStep(timeOverride);

    HashSet<Long> empireIds = new HashSet<>();
    for (Planet planet : star.planets) {
      if (planet.colony != null && !empireIds.contains(planet.colony.empire_id)) {
        empireIds.add(planet.colony.empire_id);
      }
    }

    // We'll simulate in "prediction mode" for an extra bit of time so that we can get a
    // more accurate estimate of the end time for builds. We won't *record* the population
    // growth and such, just the end time of builds. We'll also record the time that the
    // population drops below a certain threshold so that we can warn the player.
    long predictionTime = endTime + Time.DAY;
    Star.Builder predictionStar = null;
    long now = startTime;
    while (true) {
      if (now < endTime) {
        simulateStepForAllEmpires(now, star, empireIds);
      } else if (predictionStar == null) {
        // We always predict at least one more step, so that we can put the deltas from the next
        // step in (since they'll take into account things like focus changes, new builds, etc that
        // the user has applied in THIS step).
        predictionStar = star.build().newBuilder();
        log("Begin prediction");
        simulateStepForAllEmpires(now, predictionStar, empireIds);
        copyDeltas(star, predictionStar);
      } else if (predict && now < predictionTime) {
        simulateStepForAllEmpires(now, predictionStar, empireIds);
      } else {
        break;
      }
      now += STEP_TIME;
    }

    // copy the end times for builds from the prediction star
    for (int i = 0; i < star.planets.size(); i++) {
      Planet predictionPlanet = predictionStar.planets.get(i);
      Planet.Builder planet = star.planets.get(i).newBuilder();
      if (planet.colony == null || predictionPlanet.colony == null
          || planet.colony.build_requests == null
          || predictionPlanet.colony.build_requests == null) {
        continue;
      }

      ArrayList<BuildRequest> buildRequests = new ArrayList<>();
      for (BuildRequest predictionBuildRequest : predictionPlanet.colony.build_requests) {
        for (int j = 0; j < planet.colony.build_requests.size(); j++) {
          BuildRequest.Builder br = planet.colony.build_requests.get(j).newBuilder();
          if (predictionBuildRequest.id.equals(br.id)) {
            buildRequests.add(br.end_time(predictionBuildRequest.end_time).build());
          }
        }
      }
      planet.colony(planet.colony.newBuilder().build_requests(buildRequests).build());
      star.planets.set(i, planet.build());
    }
/*
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
    */

    star.last_simulation = endTime;
  }

  /**
   * After simulating one more step in the prediction star, copy the mineral, goods and energy
   * deltas across to the main star.
   */
  private void copyDeltas(Star.Builder star, Star.Builder predictionStar) {
    ArrayList<EmpireStorage> stores = new ArrayList<>();
    for (int i = 0; i < star.empire_stores.size(); i++) {
      stores.add(star.empire_stores.get(i).newBuilder()
          .minerals_delta_per_hour(predictionStar.empire_stores.get(i).minerals_delta_per_hour)
          .goods_delta_per_hour(predictionStar.empire_stores.get(i).goods_delta_per_hour)
          .energy_delta_per_hour(predictionStar.empire_stores.get(i).energy_delta_per_hour)
          .build());
    }
    star.empire_stores(stores);
  }

  /**
   * Gets the time we should start simulating this star for.
   *
   * Most of the time, this will be the time the star was last simulated, but in the case of native-
   * only stars, we'll just simulate the last 24 hours.
   */
  private long getSimulateStartTime(Star.Builder star) {
    Long lastSimulation = star.last_simulation;

    // if there's only native colonies, don't bother simulating from more than
    // 24 hours ago. The native colonies will generally be in a steady state
    long oneDayAgo = System.currentTimeMillis() - Time.DAY;
    if (lastSimulation != null && lastSimulation < oneDayAgo) {
      log("Last simulation more than on day ago, checking whether there are any non-native "
          + "colonies.");
      boolean onlyNativeColonies = true;
      for (Planet planet : star.planets) {
        if (planet.colony != null && planet.colony.empire_id != null) {
          onlyNativeColonies = false;
        }
      }
      for (Fleet fleet : star.fleets) {
        if (fleet.empire_id != null) {
          onlyNativeColonies = false;
        }
      }
      if (onlyNativeColonies) {
        log("No non-native colonies detected, simulating only 24 hours in the past.");
        lastSimulation = oneDayAgo;
      }
    }

    if (lastSimulation == null) {
      log("Star has never been simulated, simulating for 1 step only");
      lastSimulation = System.currentTimeMillis() - STEP_TIME;
    }
    return trimTimeToStep(lastSimulation);
  }

  /** Trims a time to the step time. */
  private static long trimTimeToStep(long time) {
    return (time / STEP_TIME) * STEP_TIME;
  }

  private void simulateStepForAllEmpires(long now, Star.Builder star, Set<Long> empireIds) {
    log("- Step [now=%s]", Time.format(now));
    for (Long empireId : empireIds) {
      log(String.format("-- Empire [%s]", empireId == null ? "Native" : empireId));
      simulateStep(now, star, empireId);
    }

    // Don't forget to simulate combat for this step as well.
    simulateCombat(star, now);
  }

  private void simulateStep(long now, Star.Builder star, @Nullable Long empireId) {
    float totalPopulation = 0.0f;

    EmpireStorage.Builder storage = null;
    int storageIndex = -1;
    for (int i = 0; i < star.empire_stores.size(); i++) {
      if (!equalEmpire(star.empire_stores.get(i).empire_id, empireId)) {
        continue;
      }
      storageIndex = i;
      storage = star.empire_stores.get(i).newBuilder();
    }
    if (storage == null) {
      log("No storage found for this empire!");
      return;
    }

    float dt = Time.toHours(STEP_TIME);
    float goodsDeltaPerHour = 0.0f;
    float mineralsDeltaPerHour = 0.0f;
    float energyDeltaPerHour = 0.0f;

    for (int i = 0; i < star.planets.size(); i++) {
      Planet planet = star.planets.get(i);
      if (planet.colony == null) {
        continue;
      }
      Colony.Builder colony = planet.colony.newBuilder();
      if (!equalEmpire(colony.empire_id, empireId)) {
        continue;
      }

      log("--- Colony [planetIndex=%d] [population=%.2f]", i, colony.population);

      // Calculate the output from farming this turn and add it to the star global
      float goods =
          colony.population * colony.focus.farming * (planet.farming_congeniality / 100.0f);
      colony.delta_goods(goods);
      storage.total_goods(storage.total_goods + goods * dt);
      goodsDeltaPerHour += goods;
      log("    Goods: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_goods, goods, goods * dt);

      // calculate the output from mining this turn and add it to the star global
      float minerals =
          colony.population * colony.focus.mining * (planet.mining_congeniality / 100.0f);
      colony.delta_minerals(minerals);
      storage.total_minerals(storage.total_minerals + minerals * dt);
      mineralsDeltaPerHour += minerals;
      log("    Minerals: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_minerals, minerals, minerals * dt);

      // calculate the output from energy this turn and add it to the star global
      float enegry =
          colony.population * colony.focus.energy * (planet.energy_congeniality / 100.0f);
      colony.delta_energy(enegry);
      storage.total_energy(storage.total_energy + enegry * dt);
      energyDeltaPerHour += enegry;
      log("    Energy: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_energy, enegry, enegry * dt);

      totalPopulation += colony.population;

      star.planets.set(i, planet.newBuilder().colony(colony.build()).build());
    }

    // A second loop though the colonies, once the goods/minerals have been calculated.
    for (int i = 0; i < star.planets.size(); i++) {
      Planet.Builder planet = star.planets.get(i).newBuilder();
      if (planet.colony == null) {
        continue;
      }
      Colony.Builder colony = planet.colony.newBuilder();
      if (!equalEmpire(colony.empire_id, empireId)) {
        continue;
      }
      if (colony.build_requests == null || colony.build_requests.isEmpty()) {
        continue;
      }

      // not all build requests will be processed this turn. We divide up the population
      // based on the number of ACTUAL build requests they'll be working on this turn
      int numValidBuildRequests = 0;
      for (BuildRequest br : colony.build_requests) {
        if (br.start_time > now) {
          continue;
        }
        if (br.progress >= 1.0f) {
          continue;
        }

        // as long as it's started but hasn't finished, we'll be working on it this turn
        numValidBuildRequests += 1;
      }

      // If we have pending build requests, we'll have to update them as well
      if (numValidBuildRequests > 0) {
        float totalWorkers = colony.population * colony.focus.construction;
        float workersPerBuildRequest = totalWorkers / numValidBuildRequests;
        float mineralsPerBuildRequest = storage.total_minerals / numValidBuildRequests;

        log("--- Building [buildRequests=%d] [planetIndex=%d] [totalWorker=%.2f] [totalMinerals=%.2f]",
            numValidBuildRequests, planet.index, totalWorkers, storage.total_minerals);

        // OK, we can spare at least ONE population
        if (workersPerBuildRequest < 1.0f) {
          workersPerBuildRequest = 1.0f;
        }

        ArrayList<BuildRequest> completeBuildRequests = new ArrayList<>();
        for (int j = 0; j < colony.build_requests.size(); j++) {
          BuildRequest.Builder br = colony.build_requests.get(j).newBuilder();
          Design design = DesignHelper.getDesign(br.design_type);

          long startTime = br.start_time;
          if (startTime > now || br.progress >= 1.0f) {
            completeBuildRequests.add(br.build());
            continue;
          }

          // the build cost is defined by the original design, or possibly by the upgrade if that
          // is what it is.
          Design.BuildCost buildCost = design.build_cost;
          //if (br.mExistingFleetID != null) {
          //  ShipDesign shipDesign = (ShipDesign) design;
          //  ShipDesign.Upgrade upgrade = shipDesign.getUpgrade(br.getUpgradeID());
          //  buildCost = upgrade.getBuildCost();
          //}

          log("---- Building [design=%s %s] [count=%d] cost [workers=%d] [minerals=%d]",
              design.design_kind, design.type, br.count, buildCost.population, buildCost.minerals);

          // The total amount of time to build something is based on the number of workers it
          // requires, if you have the right number of workers and the right amount of minerals,
          // you can finish the build in one turn. We require whatever fraction of progress is left
          // of both minerals and workers.
          float totalWorkersRequired = buildCost.population * (1.0f - br.progress);
          float totalMineralsRequired = buildCost.minerals * (1.0f - br.progress);
          log("     Required: [population=%.2f] [minerals=%.2f]",
              totalWorkersRequired, totalMineralsRequired);

          // The amount of work we can do this turn is the minimum of whatever resources we have
          // available allows.
          float progressThisTurn = Math.min(
              workersPerBuildRequest / totalWorkersRequired,
              mineralsPerBuildRequest / totalMineralsRequired);
          log("     Progress: [this turn=%.4f] [total=%.4f]",
              progressThisTurn, br.progress + progressThisTurn);

          // what is the current amount of time we have now as a percentage of the total build
          // time?
          if (progressThisTurn + br.progress >= 1.0f) {
            // OK, we've finished!
            log("     FINISHED!");
            br.progress(1.0f);
            br.end_time(now + STEP_TIME);
            completeBuildRequests.add(br.build());
            continue;
          }

          // work hasn't finished yet, so lets estimate how long it will take now
          float remainingWorkersRequired =
              buildCost.population * (1.0f - br.progress - progressThisTurn);
          float remainingMineralsRequired =
              buildCost.minerals * (1.0f - br.progress - progressThisTurn);

          float mineralsUsedThisTurn = totalMineralsRequired - remainingMineralsRequired;
          storage.total_minerals(storage.total_minerals - mineralsUsedThisTurn);
          mineralsDeltaPerHour -= mineralsUsedThisTurn;
          log("     Used: [minerals=%.2f]", mineralsUsedThisTurn);

          float timeForMineralsHours =
              remainingMineralsRequired / mineralsUsedThisTurn / (Time.HOUR / STEP_TIME);
          float timeForPopulationHours =
              remainingWorkersRequired / workersPerBuildRequest / (Time.HOUR / STEP_TIME);
          log("     Remaining: [minerals=%.2f hrs] [population=%.2f hrs]",
              timeForMineralsHours, timeForPopulationHours);
          br.end_time(now +
              Math.round(Math.max(timeForMineralsHours, timeForPopulationHours)) * Time.HOUR);
          br.progress(br.progress + progressThisTurn);

          completeBuildRequests.add(br.build());
        }

        star.planets.set(i, planet.colony(
            colony.build_requests(completeBuildRequests).build()).build());
      }
    }

    // Finally, update the population. The first thing we need to do is evenly distribute goods
    // between all of the colonies.
    float totalGoodsPerHour = totalPopulation / 10.0f;
    if (totalPopulation > 0.0001f && totalGoodsPerHour < 10.0f) {
      totalGoodsPerHour = 10.0f;
    }
    float totalGoodsRequired = totalGoodsPerHour * dt;
    goodsDeltaPerHour -= totalGoodsPerHour;

    // If we have more than total_goods_required stored, then we're cool. Otherwise, our population
    // suffers...
    float goodsEfficiency = 1.0f;
    if (totalGoodsRequired > storage.total_goods && totalGoodsRequired > 0) {
      goodsEfficiency = storage.total_goods / totalGoodsRequired;
    }

    log("--- Updating Population [goods required=%.2f] [goods available=%.2f] [efficiency=%.2f]",
        totalGoodsRequired, storage.total_goods, goodsEfficiency);

    // subtract all the goods we'll need
    storage.total_goods(storage.total_goods - totalGoodsRequired);
    if (storage.total_goods <= 0.0f) {
      // We've run out of goods! That's bad...
      storage.total_goods(0.0f);

      if (storage.goods_zero_time == null || storage.goods_zero_time > now) {
        log("    GOODS HAVE HIT ZERO");
        storage.goods_zero_time(now);
      }
    }

    // now loop through the colonies and update the population/goods counter
    for (int i = 0; i < star.planets.size(); i++) {
      Planet planet = star.planets.get(i);
      if (planet.colony == null) {
        continue;
      }
      Colony.Builder colony = planet.colony.newBuilder();
      if (!equalEmpire(colony.empire_id, empireId)) {
        continue;
      }

      float populationIncrease;
      if (goodsEfficiency >= 1.0f) {
        populationIncrease = Math.max(colony.population, 10.0f) * 0.1f;
      } else {
        populationIncrease = Math.max(colony.population, 10.0f);
        populationIncrease *= 0.9f;
        populationIncrease *= 0.25f * (goodsEfficiency - 1.0f);
      }

      colony.delta_population(populationIncrease);
      float populationIncreaseThisTurn = populationIncrease * dt;

      if (colony.cooldown_end_time != null && colony.cooldown_end_time < now) {
        log("    Colony is no longer in cooldown period.");
        colony.cooldown_end_time = null;
      }

      int maxPopulation = ColonyHelper.getMaxPopulation(planet);
      float newPopulation = colony.population + populationIncreaseThisTurn;
      if (newPopulation < 1.0f) {
        newPopulation = 0.0f;
      } else if (newPopulation > maxPopulation) {
        newPopulation = maxPopulation;
      }
      if (newPopulation < 100.0f && colony.cooldown_end_time != null) {
        newPopulation = 100.0f;
      }
      log("    Colony[%d]: [delta=%.2f] [new=%.2f]", i, populationIncrease, newPopulation);
      colony.population(newPopulation);

      star.planets.set(i, planet.newBuilder().colony(colony.build()).build());
    }

    if (storage.total_goods > storage.max_goods) {
      storage.total_goods = storage.max_goods;
    }
    if (storage.total_minerals > storage.max_minerals) {
      storage.total_minerals = storage.max_minerals;
    }
    if (storage.total_energy > storage.max_energy) {
      storage.total_energy = storage.max_energy;
    }

    storage.goods_delta_per_hour(goodsDeltaPerHour);
    storage.minerals_delta_per_hour(mineralsDeltaPerHour);
    storage.energy_delta_per_hour(energyDeltaPerHour);
    if (storageIndex >= 0) {
      star.empire_stores.set(storageIndex, storage.build());
    } else {
      star.empire_stores.add(storage.build());
    }
  }

  private void simulateCombat(Star.Builder star, long now) {/*
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

    if (attackStartTime == null || attackStartTime.isBefore(now)) {
      attackStartTime = now;
    }

    // round up to the next minute
    attackStartTime = attackStartTime.withSecondOfMinute(0).withMillisOfSecond(0);
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
    }*/
  }
/*
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
  }*/

  /**
   * Searches for an enemy fleet with the lowest priority.
   */
  /*private BaseCombatReport.FleetSummary findTarget(BaseCombatReport.CombatRound round,
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
  }*/

  private static boolean equalEmpire(Long one, Long two) {
    if (one == null && two == null) {
      return true;
    }
    if (one == null || two == null) {
      return false;
    }
    return one.equals(two);
  }
/*
  private static boolean isFriendly(BaseCombatReport.FleetSummary fleet1, BaseCombatReport.FleetSummary fleet2) {
    BaseFleet baseFleet1 = fleet1.getFleets().get(0);
    BaseFleet baseFleet2 = fleet2.getFleets().get(0);
    return isFriendly(baseFleet1, baseFleet2);
  }
*//*
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
        fleet1.getAllianceID().intValue() == fleet2.getAllianceID()) {
      return true;
    }

    // otherwise they're enemies
    return false;
  }
*//*
  private boolean isDestroyed(BaseFleet fleet, DateTime now) {
    if (fleet.getTimeDestroyed() != null && !fleet.getTimeDestroyed().isAfter(now)) {
      return true;
    }
    return false;
  }
*/
  private void log(String format, Object... args) {
    if (logHandler != null) {
      logHandler.log(String.format(Locale.US, format, args));
    }
  }

  /**
   * This interface is used to help debug the simulation code. Implement it to receive a bunch
   * of debug log messages during the simulation process.
   */
  public interface LogHandler {
    void setStarName(String starName);
    void log(String message);
  }

  private static class BasicLogHandler implements LogHandler {
    private static final Log log = new Log("Simulation");
    private String starName;

    @Override
    public void setStarName(String starName) {
      this.starName = starName;
    }

    @Override
    public void log(String message) {
      log.info(starName + " - " + message);
    }
  }
}
