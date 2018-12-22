package au.com.codeka.warworlds.common.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.CombatReport;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

import static au.com.codeka.warworlds.common.sim.SimulationHelper.trimTimeToStep;

/** This class is used to simulate a {@link Star}. */
public class Simulation {
  private final LogHandler logHandler;
  private final boolean predict;
  private long timeOverride;

  private static final boolean sDebug = false;

  /**
   * A small floating-point value, if you have fewer ships than this, then you basically have none.
   */
  private static final double EPSILON = 0.1;

  /** Step time is 10 minutes. */
  public static final long STEP_TIME = 10 * Time.MINUTE;

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
    log("Begin simulation for '%s' @ %d", star.name, timeOverride);

    // figure out the start time, which is the oldest last_simulation time
    long startTime = getSimulateStartTime(star);
    long endTime = trimTimeToStep(timeOverride);

    HashSet<Long> empireIds = new HashSet<>();
    for (Planet planet : star.planets) {
      if (planet.colony != null) {
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
      if (now <= endTime) {
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
      } else if (predict && now <= predictionTime) {
        simulateStepForAllEmpires(now, predictionStar, empireIds);
      } else {
        break;
      }
      now += STEP_TIME;
    }

    // Copy the end times for builds from the prediction star.
    long stepStartTime = trimTimeToStep(timeOverride);
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
            br.end_time(predictionBuildRequest.end_time);
            buildRequests.add(br.build());
          }
        }
      }
      planet.colony(planet.colony.newBuilder().build_requests(buildRequests).build());
      star.planets.set(i, planet.build());
    }

    star.last_simulation = timeOverride;
  }

  /**
   * After simulating the first step in the prediction star, copy the mineral, goods and energy
   * deltas across to the main star. Also copy the build request efficiencies, since they're also
   * the ones we'll care about (i.e. the efficient for the *next* step).
   */
  private void copyDeltas(Star.Builder star, Star.Builder predictionStar) {
    ArrayList<EmpireStorage> stores = new ArrayList<>();
    for (int i = 0; i < star.empire_stores.size(); i++) {
      stores.add(predictionStar.empire_stores.get(i).newBuilder().build());
    }
    star.empire_stores(stores);

    for (int i = 0; i < star.planets.size(); i++) {
      Planet.Builder planetBuilder = star.planets.get(i).newBuilder();
      if (planetBuilder.colony != null) {
        Colony.Builder colonyBuilder = planetBuilder.colony.newBuilder();
        for (int j = 0; j < planetBuilder.colony.build_requests.size(); j++) {
          BuildRequest.Builder brBuilder = colonyBuilder.build_requests.get(j).newBuilder();
          BuildRequest predictionBuildRequest =
              predictionStar.planets.get(i).colony.build_requests.get(j);
          brBuilder.minerals_efficiency(predictionBuildRequest.minerals_efficiency);
          brBuilder.population_efficiency(predictionBuildRequest.population_efficiency);
          brBuilder.progress_per_step(predictionBuildRequest.progress_per_step);
          brBuilder.delta_minerals_per_hour(predictionBuildRequest.delta_minerals_per_hour);
          colonyBuilder.build_requests.set(j, brBuilder.build());
        }
        planetBuilder.colony(colonyBuilder.build());
      }
      star.planets.set(i, planetBuilder.build());
    }
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
    long oneDayAgo = timeOverride - Time.DAY;
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
      return trimTimeToStep(timeOverride);
    }

    return trimTimeToStep(lastSimulation) + STEP_TIME;
  }

  private void simulateStepForAllEmpires(long now, Star.Builder star, Set<Long> empireIds) {
    log("- Step [now=%d]", now);
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

    storage.max_goods = 1000.0f;
    storage.max_energy = 1000.0f;
    storage.max_minerals = 1000.0f;

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

      // Apply storage bonuses this round (note: this could change from step to step, if a building
      // finishes being built, for example).
      for (Building building : colony.buildings) {
        Design design = DesignHelper.getDesign(building.design_type);
        List<Design.Effect> effects = design.effect;
        if (building.level != null && building.level > 1) {
          effects = design.upgrades.get(building.level).effects;
        }
        for (Design.Effect effect : effects) {
          if (effect.type == Design.EffectType.STORAGE) {
            storage.max_goods += effect.goods;
            storage.max_minerals += effect.minerals;
            storage.max_energy += effect.energy;
          }
        }
      }

      // Some sanity checks.
      if (Float.isNaN(storage.total_energy) || Float.isInfinite(storage.total_energy)) {
        storage.total_energy = 0.0f;
      }
      if (Float.isNaN(storage.total_goods) || Float.isInfinite(storage.total_goods)) {
        storage.total_goods = 0.0f;
      }
      if (Float.isNaN(storage.total_minerals) || Float.isInfinite(storage.total_minerals)) {
        storage.total_minerals = 0.0f;
      }

      if (Float.isNaN(colony.focus.construction) || Float.isInfinite(colony.focus.construction) ||
          Float.isNaN(colony.focus.farming) || Float.isInfinite(colony.focus.farming) ||
          Float.isNaN(colony.focus.mining) || Float.isInfinite(colony.focus.mining) ||
          Float.isNaN(colony.focus.energy) || Float.isInfinite(colony.focus.energy)) {
        colony.focus(colony.focus.newBuilder()
            .construction(0.25f).energy(0.25f).farming(0.25f).mining(0.25f).build());
      }

      log("--- Colony [planetIndex=%d] [population=%.2f]", i, colony.population);

      // Calculate the output from farming this turn and add it to the star global
      float goods =
          colony.population * colony.focus.farming * (planet.farming_congeniality / 100.0f);
      colony.delta_goods(goods);
      storage.total_goods(Math.max(0, storage.total_goods + goods * dt));
      goodsDeltaPerHour += goods;
      log("    Goods: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_goods, goods, goods * dt);

      // calculate the output from mining this turn and add it to the star global
      float mineralsPerHour =
          colony.population * colony.focus.mining * (planet.mining_congeniality / 100.0f);
      colony.delta_minerals(mineralsPerHour);
      storage.total_minerals(Math.max(0, storage.total_minerals + mineralsPerHour * dt));
      mineralsDeltaPerHour += mineralsPerHour;
      log("    Minerals: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_minerals, mineralsPerHour, mineralsPerHour * dt);

      // calculate the output from energy this turn and add it to the star global
      float energy =
          colony.population * colony.focus.energy * (planet.energy_congeniality / 100.0f);
      colony.delta_energy(energy);
      storage.total_energy(Math.max(0, storage.total_energy + energy * dt));
      energyDeltaPerHour += energy;
      log("    Energy: [total=%.2f] [delta=%.2f / hr] [this turn=%.2f]",
          storage.total_energy, energy, energy * dt);

      totalPopulation += colony.population;

      star.planets.set(i, planet.newBuilder().colony(colony.build()).build());
    }

    // A second loop though the colonies, once the goods/minerals/energy has been calculated.
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
        if (br.start_time >= now) {
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
          // Sanity check.
          if (Float.isNaN(br.progress)) {
            br.progress(0.0f);
          }
          Design design = DesignHelper.getDesign(br.design_type);

          long startTime = br.start_time;
          if (startTime > now || br.progress >= 1.0f) {
            completeBuildRequests.add(br.build());
            continue;
          }

          // the build cost is defined by the original design, or possibly by the upgrade if that
          // is what it is.
          Design.BuildCost buildCost = design.build_cost;
          if (br.building_id != null) {
            for (Building building : colony.buildings) {
              if (Objects.equals(building.id, br.building_id)) {
                buildCost = design.upgrades.get(building.level - 1).build_cost;
                break;
              }
            }
          }
          //if (br.mExistingFleetID != null) {
          //  ShipDesign shipDesign = (ShipDesign) design;
          //  ShipDesign.Upgrade upgrade = shipDesign.getUpgrade(br.getUpgradeID());
          //  buildCost = upgrade.getBuildCost();
          //}

          log("---- Building [design=%s %s] [count=%d] cost [workers=%d] [minerals=%d] [start-time=%d]",
              design.design_kind, design.type, br.count, buildCost.population, buildCost.minerals,
              br.start_time);

          // The total amount of time to build something is based on the number of workers it
          // requires, if you have the right number of workers and the right amount of minerals,
          // you can finish the build in one turn. However, if you only have a fraction of them
          // available, then that fraction of progress will be made.
          float totalWorkersRequired = buildCost.population * br.count;
          float totalMineralsRequired = buildCost.minerals * br.count;
          log("     Required: [population=%.2f] [minerals=%.2f]",
              totalWorkersRequired, totalMineralsRequired);
          log("     Available: [population=%.2f] [minerals=%.2f]",
              workersPerBuildRequest, mineralsPerBuildRequest);

          // The amount of work we can do this turn is based on how much population we have (if
          // we have enough minerals) or based the min of population/minerals if we don't.
          float populationProgressThisTurn = workersPerBuildRequest / totalWorkersRequired;
          float mineralsProgressThisTurn = mineralsPerBuildRequest / totalMineralsRequired;
          float progressThisTurn =
              mineralsProgressThisTurn >= 1.0
                  ? populationProgressThisTurn
                  : Math.min(populationProgressThisTurn, mineralsProgressThisTurn);
          log("     Progress: [this turn=%.4f (minerals=%.4f pop=%.4f] [total=%.4f]",
              progressThisTurn,
              mineralsProgressThisTurn,
              populationProgressThisTurn,
              br.progress + progressThisTurn);

          // If it started half way through this step, the progress is lessened.
          if (br.start_time > now) {
            float fraction = ((float) startTime - now) / STEP_TIME;
            progressThisTurn *= fraction;
            log("     Reduced progress: %.2f (fraction=%.2f)", progressThisTurn, fraction);
          }

          float mineralsUsedThisTurn = Math.min(totalMineralsRequired, mineralsPerBuildRequest);
          if (populationProgressThisTurn < mineralsProgressThisTurn) {
            // If we're limited by population, then we won't have used all of the minerals that
            // were available to us.
            mineralsUsedThisTurn *= populationProgressThisTurn;
          }
          storage.total_minerals(Math.max(0, storage.total_minerals - mineralsUsedThisTurn));
          br.delta_minerals_per_hour(-mineralsUsedThisTurn * STEP_TIME / Time.HOUR);
          log("     Used: [minerals=%.4f]", mineralsUsedThisTurn);

          // what is the current amount of time we have now as a percentage of the total build
          // time?
          if (progressThisTurn + br.progress >= 1.0f) {
            // OK, we've finished! Work out how far into the step we completed.
            float unusedProgress = progressThisTurn + br.progress - 1.0f;
            float fractionProgress = (progressThisTurn - unusedProgress) / progressThisTurn;
            long endTime = now - STEP_TIME;
            if (br.start_time > endTime) {
              endTime = br.start_time;
            }
            endTime += (long)(STEP_TIME * fractionProgress);

            log("     FINISHED! progress-this-turn: %.2f fraction-progress = %.2f, end-time=%d",
                progressThisTurn, fractionProgress, endTime);
            br.progress(1.0f);
            br.progress_per_step(progressThisTurn);
            br.end_time(endTime);
            completeBuildRequests.add(br.build());
            continue;
          }

          // work hasn't finished yet, so lets estimate how long it will take now
          float remainingWorkersRequired =
              buildCost.population * (1.0f - br.progress - progressThisTurn) * br.count;
          float remainingMineralsRequired =
              buildCost.minerals * (1.0f - br.progress - progressThisTurn) * br.count;

          float timeForMineralsSteps =
              (remainingMineralsRequired / mineralsPerBuildRequest);
          float timeForPopulationSteps =
              (remainingWorkersRequired / workersPerBuildRequest);
          float timeForMineralsHours = timeForMineralsSteps * STEP_TIME / Time.HOUR;
          float timeForPopulationHours = timeForPopulationSteps * STEP_TIME / Time.HOUR;
          log("     Remaining: [minerals=%.2f hrs %.2f steps] [population=%.2f hrs %.2f steps]",
              timeForMineralsHours,
              timeForMineralsSteps,
              timeForPopulationHours,
              timeForPopulationSteps);
          br.end_time(now +
              Math.round(Math.max(timeForMineralsHours, timeForPopulationHours) * Time.HOUR));
          log("     Finish time: %d (now=%d)",
              now + Math.round(Math.max(timeForMineralsHours, timeForPopulationHours) * Time.HOUR),
              now);
          br.progress(br.progress + progressThisTurn);
          br.progress_per_step(progressThisTurn);

          // Calculate the efficiency of the minerals vs. population
          float sumTimeInHours = timeForMineralsHours + timeForPopulationHours;
          float mineralsEfficiency = 1 - (timeForMineralsHours / sumTimeInHours);
          float populationEfficiency = 1 - (timeForPopulationHours / sumTimeInHours);
          br.minerals_efficiency(mineralsEfficiency);
          br.population_efficiency(populationEfficiency);
          log("     Efficiency: [minerals=%.3f] [population=%.3f]",
              mineralsEfficiency, populationEfficiency);

          completeBuildRequests.add(br.build());
        }

        star.planets.set(i, planet.colony(
            colony.build_requests(completeBuildRequests).build()).build());
      }
    }

    // Now loop through the fleets and see if there's any that needs more fuel. Fill 'em up if there
    // is.
    for (int i = 0; i < star.fleets.size(); i++) {
      if (storage.total_energy < 0.001f) {
        break;
      }

      Fleet.Builder fleet = star.fleets.get(i).newBuilder();
      if (!equalEmpire(fleet.empire_id, empireId)) {
        continue;
      }

      // TODO: this shouldn't happen normally, it's just for fleets that we added before adding fuel
      // to the game.
      if (fleet.fuel_amount == null) {
        fleet.fuel_amount(0.0f);
      }

      Design design = DesignHelper.getDesign(fleet.design_type);
      float neededFuelTotal = (float) design.fuel_size * fleet.num_ships;
      if (fleet.fuel_amount < neededFuelTotal) {
        float neededFuelRemaining = neededFuelTotal - fleet.fuel_amount;
        float actual = Math.min(neededFuelRemaining, storage.total_energy);
        fleet.fuel_amount += actual;
        storage.total_energy -= actual;
        log("--- Fleet %d [%s x %.0f] re-fueling: %.2f",
            fleet.id, design.display_name, fleet.num_ships, actual);
      }

      star.fleets.set(i, fleet.build());
    }

    // Finally, update the population. The first thing we need to do is evenly distribute goods
    // between all of the colonies.
    float totalGoodsPerHour = Math.min(10.0f, totalPopulation / 10.0f);
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
    star.empire_stores.set(storageIndex, storage.build());
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
    log("Begin combat for '%s'", star.name);

    int roundNumber = 1;
    do {
      log(" - Combat round %d", roundNumber);
      simulateCombatRound(star, now);
      roundNumber ++;
    } while (anyFleetsAttacking(star));

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
        log("   - Fleet=[%d %s] numShips=%.2f", fleet.id, design.display_name, fleet.num_ships);
        while (attack > 0.0) {
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
            double previousDamage = 0.0;
            if (damageCounter.containsKey(target.id)) {
              previousDamage = damageCounter.get(target.id);
              numShips -= previousDamage;
            }
            log("      Target=[%d] numShips=%.4f * %.2f <-- attack=%.4f",
                target.id, numShips, design.base_defence, attack);
            numShips *= design.base_defence;
            if (numShips >= attack) {
              // If there's more ships than we have attack capability, just apply the damage.
              damageCounter.put(target.id, previousDamage + (attack / design.base_defence));
              attack -= numShips;
            } else {
              // If we have more attack capability than they have ships, they're dead.
              damageCounter.put(target.id, (double) target.num_ships);
              attack = 0;
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

        if (damage - fleet.num_ships <= EPSILON) {
          log("      Fleet=%d destroyed (num_ships=%.4f <= damage=%.4f).",
              fleet.id, fleet.num_ships, damage);
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
          log("      Fleet=%d numShips=%.8f damage=%.8f state=%s.",
              fleet.id, fleet.num_ships, damage, state);
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

      // If it's destroyed, it's not a good target.
      if (potentialTarget.is_destroyed != null && potentialTarget.is_destroyed) {
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
