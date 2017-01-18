package au.com.codeka.warworlds.common.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.CombatReport;
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
        // This is also the time to simulate combat. The star has been simulated up to "now", combat
        // can run, and then we can do the first prediction once combat has completed.
        simulateCombat(star, now);

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
   * After simulating the first step in the prediction star, copy the mineral, goods and energy
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

  /**
   * Simulate combat on the star.
   *
   * <p>Combat runs in rounds, but rounds do not take any "time". Each round every fleet that is
   * attacking find a target and attacks it. The number of fleets destroyed by the attack is
   * simply the attacking fleet's attack stat multiplied by the number of ships, divided by the
   * defending fleet's defense stat.
   *
   * <p>If a fleet is destroyed by the attack, the remaining attack points are then used to target
   * another fleet in the same round until there's no more attack points left. This is so that you
   * get an advantage by splitting up all your fleets.
   */
  private void simulateCombat(Star.Builder star, long now) {
    // if there's no fleets in ATTACKING mode, then there's nothing to do
    if (!anyFleetsAttacking(star)) {
      return;
    }

    // Create a new combat report, and save the current fleets to it.
    CombatReport.Builder combatReportBuilder = new CombatReport.Builder()
        .time(now)
        .fleets_before(new ArrayList<>(star.fleets));
    log("-- Combat has begun.");

    int roundNumber = 1;
    do {
      log("   Combat round %d", roundNumber);
      simulateCombatRound(star, now);
      roundNumber ++;
    } while (anyFleetsAttacking(star));
    log("   Combat is complete.");

    // Add the combat report to the star, and remove any if there's more than 10 in the history.
    combatReportBuilder.fleets_after(new ArrayList<>(star.fleets));
    star.combat_reports.add(0, combatReportBuilder.build());
    while (star.combat_reports.size() > 10) {
      star.combat_reports.remove(star.combat_reports.size() - 1);
    }
  }

  private boolean anyFleetsAttacking(Star.Builder star) {
    int numAttacking = 0;
    for (Fleet fleet : star.fleets) {
      if (fleet.state != Fleet.FLEET_STATE.ATTACKING
          || (fleet.is_destroyed != null && fleet.is_destroyed)) {
        continue;
      }
      numAttacking ++;
    }
    return numAttacking > 0;
  }

  /** Simulate a single round of combat on the given star. */
  private void simulateCombatRound(Star.Builder star, long now) {
    Map<Long, Double> damageCounter = new HashMap<>();

    for (int i = 0; i < star.fleets.size(); i++) {
      Fleet fleet = star.fleets.get(i);
      if (fleet.state == Fleet.FLEET_STATE.ATTACKING) {
        // Work out how much attacking power this fleet has.
        Design design = DesignHelper.getDesign(fleet.design_type);
        double attack = fleet.num_ships * design.base_attack;
        while (attack > 0.0) {
          log("   -- Fleet=[%d %s] attack=%.4f", fleet.id, design.display_name, attack);
          Fleet target = findTarget(star, fleet, damageCounter);
          if (target == null) {
            log("      No target.");
            // No target was found, there's nothing left to attack.
            star.fleets.set(i, star.fleets.get(i).newBuilder()
                .state(Fleet.FLEET_STATE.IDLE)
                .state_start_time(now)
                .build());
            break;
          } else {
            // Got a target, work out how much damage this fleet has already taken.
            Double numShips = (double) target.num_ships;
            if (damageCounter.containsKey(target.id)) {
              numShips -= damageCounter.get(target.id);
            }
            log("      Target=[%d] numShips=%.4f", target.id, numShips);
            if (numShips <= attack) {
              // If there's more ships than we have attack capability, just apply the damage.
              attack -= numShips;
              damageCounter.put(target.id, (double) target.num_ships);
            } else {
              // If we have more attack capability than they have ships, they're dead.
              attack = 0;
              damageCounter.put(target.id, numShips - attack);
            }
          }
        }
      }
    }

    if (damageCounter.size() > 0) {
      log("   -- Applying damage...");
      // Now that everyone has attacked, apply the damage.
      for (int i = 0; i < star.fleets.size(); i++) {
        Fleet fleet = star.fleets.get(i);
        Double damage = damageCounter.get(fleet.id);
        if (damage == null) {
          continue;
        }

        if (fleet.num_ships <= damage) {
          log("      Fleet=%d destroyed.", fleet.id);
          star.fleets.set(i, fleet.newBuilder()
              .is_destroyed(true)
              .num_ships(0.0f)
              .build());
        } else {
          // They'll be attacking next round (unless their stance is passive).
          Fleet.FLEET_STATE state = Fleet.FLEET_STATE.ATTACKING;
          if (fleet.stance == Fleet.FLEET_STANCE.PASSIVE) {
            state = fleet.state;
          }
          log("      Fleet=%d numShips=%.4f state=%s.",
              fleet.id, fleet.num_ships - (float) (double) damage, state);
          star.fleets.set(i, fleet.newBuilder()
              .num_ships(fleet.num_ships - (float) (double) damage)
              .state(state)
              .build());
        }
      }
    } else {
      log("   -- No damage to apply.");
    }
  }

  /**
   * Searches for an enemy fleet with the lowest priority.
   *
   * @param star The star we're searching on.
   * @param fleet The fleet we're searching for a target for.
   * @param damageCounter A mapping of fleet IDs to the damage they've taken so far this round (so
   *                      that we don't target them if they're already destroyed).
   */
  @Nullable
  private Fleet findTarget(Star.Builder star, Fleet fleet, Map<Long, Double> damageCounter) {
    int foundPriority = 9999;
    Fleet target = null;

    for (Fleet potentialTarget : star.fleets) {
      // If it's moving we can't attack it.
      if (potentialTarget.state == Fleet.FLEET_STATE.MOVING) {
        continue;
      }

      // If it's friendly, we can't attack it,
      if (FleetHelper.isFriendly(fleet, potentialTarget)) {
        continue;
      }

      // If it's already destroyed, we can't attack it.
      Double damage = damageCounter.get(potentialTarget.id);
      if (damage != null && damage >= potentialTarget.num_ships) {
        continue;
      }

      // If its priority is higher than the one we've already found, then don't bother.
      Design design = DesignHelper.getDesign(potentialTarget.design_type);
      if (design.combat_priority > foundPriority) {
        continue;
      }

      foundPriority = design.combat_priority;
      target = potentialTarget;
    }

    return target;
  }

  private static boolean equalEmpire(Long one, Long two) {
    if (one == null && two == null) {
      return true;
    }
    if (one == null || two == null) {
      return false;
    }
    return one.equals(two);
  }

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
