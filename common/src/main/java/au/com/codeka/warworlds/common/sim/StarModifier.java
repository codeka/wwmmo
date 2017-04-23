package au.com.codeka.warworlds.common.sim;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.ColonyFocus;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;

/**
 * Class for handling modifications to a star.
 */
public class StarModifier {
  private static final Log log = new Log("StarModifier");

  private static final long HOURS_MS = 3600000L;

  public interface IdentifierGenerator {
    long nextIdentifier();
  }

  private final IdentifierGenerator identifierGenerator;

  public StarModifier(IdentifierGenerator identifierGenerator) {
    this.identifierGenerator = identifierGenerator;
  }

  public void modifyStar(Star.Builder star, StarModification modification) {
    modifyStar(star, null, Lists.newArrayList(modification), null);
  }

  public void modifyStar(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    modifyStar(star, null, Lists.newArrayList(modification), logHandler);
  }

  /**
   * Modify a star, and possibly other auxiliary stars.
   *
   * @param star The {@link Star.Builder} that we're modifying. The star is simulated before and
   *             after being modified.
   * @param auxStars A collection of auxiliary stars that we may need while modifying this star (for
   *                 example, MOVE_FLEET needs to know about the destination). These are not
   *                 simulated.
   * @param modifications The list of {@link StarModification}s to apply.
   * @param logHandler An optional {@link Simulation.LogHandler} that we'll pass through all log
   *                   messages to.
   */
  public void modifyStar(
      Star.Builder star,
      @Nullable Collection<Star> auxStars,
      Collection<StarModification> modifications,
      @Nullable Simulation.LogHandler logHandler) {
    if (logHandler == null) {
      logHandler = new Simulation.LogHandler() {
        @Override
        public void setStarName(String starName) {
        }

        @Override
        public void log(String message) {
        }
      };
    }
    logHandler.log("Applying " + modifications.size() + " modifications.");

    if (modifications.size() > 0) {
      new Simulation(false).simulate(star);
      for (StarModification modification : modifications) {
        applyModification(star, auxStars, modification, logHandler);
      }
    }
    new Simulation(logHandler).simulate(star);
  }

  private void applyModification(
      Star.Builder star,
      @Nullable Collection<Star> auxStars,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    switch (modification.type) {
      case COLONIZE:
        applyColonize(star, modification, logHandler);
        return;
      case CREATE_FLEET:
        applyCreateFleet(star, modification, logHandler);
        return;
      case CREATE_BUILDING:
        applyCreateBuilding(star, modification, logHandler);
        break;
      case ADJUST_FOCUS:
        applyAdjustFocus(star, modification, logHandler);
        return;
      case ADD_BUILD_REQUEST:
        applyAddBuildRequest(star, modification, logHandler);
        return;
      case DELETE_BUILD_REQUEST:
        applyDeleteBuildRequest(star, modification, logHandler);
        return;
      case SPLIT_FLEET:
        applySplitFleet(star, modification, logHandler);
        return;
      case MERGE_FLEET:
        applyMergeFleet(star, modification, logHandler);
        return;
      case MOVE_FLEET:
        applyMoveFleet(star, auxStars, modification, logHandler);
        return;
      case EMPTY_NATIVE:
        applyEmptyNative(star, modification, logHandler);
        return;
      default:
        logHandler.log("Unknown or unexpected modification type: " + modification.type);
        log.error("Unknown or unexpected modification type: %s", modification.type);
    }
  }

  private void applyColonize(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.COLONIZE));
    logHandler.log(String.format(Locale.US, "- colonizing planet #%d", modification.planet_index));

    // Destroy a colony ship, unless this is a native colony.
    if (modification.empire_id != null) {
      boolean found = false;
      int population = 100;
      for (int i = 0; i < star.fleets.size(); i++) {
        Fleet fleet = star.fleets.get(i);
        if (fleet.design_type.equals(Design.DesignType.COLONY_SHIP)
            && fleet.empire_id.equals(modification.empire_id)) {
          // TODO: check for cyrogenics
          if (Math.ceil(fleet.num_ships) == 1.0f) {
            star.fleets.remove(i);
          } else {
            star.fleets.set(i, fleet.newBuilder()
                .num_ships(fleet.num_ships - 1)
                .build());
          }
          found = true;
          break;
        }
      }

      if (!found) {
        logHandler.log("  no colonyship, cannot colonize.");
        return;
      }
    }

    star.planets.set(
        modification.planet_index,
        star.planets.get(modification.planet_index).newBuilder()
            .colony(new Colony.Builder()
                .cooldown_end_time(System.currentTimeMillis() + (15 * Time.MINUTE))
                .empire_id(modification.empire_id)
                .focus(new ColonyFocus.Builder()
                    .construction(0.1f)
                    .energy(0.3f)
                    .farming(0.3f)
                    .mining(0.3f)
                    .build())
                .id(identifierGenerator.nextIdentifier())
                .population(100.0f)
                .defence_bonus(1.0f)
                .build())
            .build());
    logHandler.log(String.format(Locale.US,
        "  colonized: colony_id=%d",
        star.planets.get(modification.planet_index).colony.id));

    // if there's no storage for this empire, add one with some defaults now.
    boolean hasStorage = false;
    for (EmpireStorage storage : star.empire_stores) {
      if (Objects.equals(storage.empire_id, modification.empire_id)) {
        hasStorage = true;
      }
    }
    if (!hasStorage) {
      star.empire_stores.add(new EmpireStorage.Builder()
          .empire_id(modification.empire_id)
          .total_goods(100.0f).total_minerals(100.0f).total_energy(1000.0f)
          .max_goods(1000.0f).max_minerals(1000.0f).max_energy(1000.0f)
          .build());
    }
  }

  private void applyCreateFleet(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.CREATE_FLEET));

    boolean attack = false;
    if (modification.fleet == null || modification.fleet.stance == Fleet.FLEET_STANCE.AGGRESSIVE) {
      for (Fleet fleet : star.fleets) {
        if (!FleetHelper.isFriendly(fleet, modification.empire_id)) {
          attack = true;
        }
      }
    }

    // First, if there's any fleets that are not friendly and aggressive, they should change to
    // attacking as well.
    int numAttacking = 0;
    for (int i = 0; i < star.fleets.size(); i++) {
      Fleet fleet = star.fleets.get(i);
      if (!FleetHelper.isFriendly(fleet, modification.empire_id)
          && fleet.stance == Fleet.FLEET_STANCE.AGGRESSIVE) {
        star.fleets.set(i, fleet.newBuilder()
            .state(Fleet.FLEET_STATE.ATTACKING)
            .state_start_time(System.currentTimeMillis())
            .build());
        numAttacking++;
      }
    }

    // Now add the fleet itself.
    logHandler.log(String.format(Locale.US, "- creating fleet (%s) numAttacking=%d",
        attack ? "attacking" : "not attacking",
        numAttacking));
    if (modification.fleet != null) {
      star.fleets.add(modification.fleet.newBuilder()
          .id(identifierGenerator.nextIdentifier())
          .state(attack ? Fleet.FLEET_STATE.ATTACKING : Fleet.FLEET_STATE.IDLE)
          .state_start_time(System.currentTimeMillis())
          .destination_star_id(null)
          .build());
    } else {
      star.fleets.add(new Fleet.Builder()
          //TODO: .alliance_id()
          .design_type(modification.design_type)
          .empire_id(modification.empire_id)
          .id(identifierGenerator.nextIdentifier())
          .num_ships((float) modification.count)
          .stance(Fleet.FLEET_STANCE.AGGRESSIVE)
          .state(attack ? Fleet.FLEET_STATE.ATTACKING : Fleet.FLEET_STATE.IDLE)
          .state_start_time(System.currentTimeMillis())
          .build());
    }
  }

  private void applyCreateBuilding(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.CREATE_BUILDING));

    Planet planet = getPlanetWithColony(star, modification.colony_id);
    if (planet != null) {
      logHandler.log(
          String.format(Locale.US, "- creating building, colony_id=%d", modification.colony_id));
      Colony.Builder colony = planet.colony.newBuilder();
      colony.buildings.add(new Building.Builder()
          .design_type(modification.design_type)
          .level(1)
          .build());
      star.planets.set(planet.index, planet.newBuilder()
          .colony(colony.build())
          .build());
    } else {
      // TODO: suspicious!
    }
  }

  private void applyAdjustFocus(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.ADJUST_FOCUS));

    Planet planet = getPlanetWithColony(star, modification.colony_id);
    if (planet != null) {
      logHandler.log("- adjusting focus.");
      star.planets.set(planet.index, planet.newBuilder()
          .colony(planet.colony.newBuilder()
              .focus(modification.focus)
              .build())
          .build());
    } else {
      // TODO: suspicious!
    }
  }

  private void applyAddBuildRequest(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST));

    Planet planet = getPlanetWithColony(star, modification.colony_id);
    if (planet != null) {
      logHandler.log("- adding build request");
      Colony.Builder colonyBuilder = planet.colony.newBuilder();
      colonyBuilder.build_requests.add(new BuildRequest.Builder()
          .id(identifierGenerator.nextIdentifier())
          .design_type(modification.design_type)
          .start_time(System.currentTimeMillis())
          .count(modification.count)
          .progress(0.0f)
          .build());
      star.planets.set(planet.index, planet.newBuilder()
          .colony(colonyBuilder.build())
          .build());
    } else {
      // TODO: suspicious!
    }
  }

  private void applyDeleteBuildRequest(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.DELETE_BUILD_REQUEST));

    Planet planet = null;
    BuildRequest buildRequest = null;
    for (Planet p : star.planets) {
      if (p.colony != null) {
        for (BuildRequest br : p.colony.build_requests) {
          if (br.id.equals(modification.build_request_id)) {
            if (!p.colony.empire_id.equals(modification.empire_id)) {
              // TODO: suspicious!
              logHandler.log("! trying to delete wrong empire's build request!");
              return;
            }
            planet = p;
            buildRequest = br;
            break;
          }
        }
        if (planet != null) {
          break;
        }
      }
    }

    if (planet == null) {
      // TODO: suspicious? (maybe complete)
      logHandler.log("Couldn't find build request with ID " + modification.build_request_id);
      return;
    }
    final long idToDelete = buildRequest.id;

    logHandler.log("- deleting build request");
    Colony.Builder colonyBuilder = planet.colony.newBuilder();
    colonyBuilder.build_requests(
        Lists.newArrayList(
            Iterables.filter(planet.colony.build_requests, new Predicate<BuildRequest>() {
              @Override
              public boolean apply(@Nullable BuildRequest br) {
                return !br.id.equals(idToDelete);
              }
            })));
    star.planets.set(planet.index, planet.newBuilder()
        .colony(colonyBuilder.build())
        .build());
  }

  private void applySplitFleet(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.SPLIT_FLEET));

    int fleetIndex = findFleetIndex(star, modification.fleet_id);
    if (fleetIndex >= 0) {
      logHandler.log("- splitting fleet");
      // Modify the existing fleet to change it's number of ships
      Fleet.Builder fleet = star.fleets.get(fleetIndex).newBuilder();
      star.fleets.set(fleetIndex, fleet
          .num_ships(fleet.num_ships - modification.count)
          .build());

      // Add a new fleet, that's a copy of the existing fleet, but with the new number of ships.
      fleet = star.fleets.get(fleetIndex).newBuilder();
      star.fleets.add(fleet
          .id(identifierGenerator.nextIdentifier())
          .num_ships((float) modification.count)
          .build());
    } else {
      // TODO: suspicious!
    }
  }

  private void applyMergeFleet(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.MERGE_FLEET));

    int fleetIndex = findFleetIndex(star, modification.fleet_id);
    if (fleetIndex >= 0) {
      Fleet.Builder fleet = star.fleets.get(fleetIndex).newBuilder();
      if (fleet.state != Fleet.FLEET_STATE.IDLE) {
        logHandler.log(String.format(Locale.US,
            "  main fleet %d is %s, cannot merge.", fleet.id, fleet.state));
      }

      for (int i = 0; i < star.fleets.size(); i++) {
        Fleet thisFleet = star.fleets.get(i);
        if (thisFleet.id.equals(fleet.id)) {
          continue;
        }
        if (modification.additional_fleet_ids.contains(thisFleet.id)) {
          if (!thisFleet.design_type.equals(fleet.design_type)) {
            // TODO: suspicious! wrong fleet type.
            logHandler.log(String.format(Locale.US,
                "  fleet #%d not the same design_type as #%d (%s vs. %s)",
                thisFleet.id, fleet.id, thisFleet.design_type, fleet.design_type));
            return;
          }

          if (thisFleet.state != Fleet.FLEET_STATE.IDLE) {
            // Suspicious? nah, just skip it.
            logHandler.log(String.format(Locale.US,
                "  fleet %d is %s, cannot merge.", thisFleet.id, thisFleet.state));
            continue;
          }

          // TODO: make sure it has the same upgrades, otherwise we have to remove it.

          fleet.num_ships(fleet.num_ships + thisFleet.num_ships);
          logHandler.log(String.format(Locale.US,
              "  removing fleet %d (num_ships=%.2f)", thisFleet.id, thisFleet.num_ships));

          // Remove this fleet, and keep going.
          star.fleets.remove(i);
          i--;
        }
      }

      // fleetIndex might've changed since we've been deleting fleets.
      logHandler.log(String.format(Locale.US,
          "  updated fleet count of main fleet: %.2f", fleet.num_ships));
      fleetIndex = findFleetIndex(star, fleet.id);
      star.fleets.set(fleetIndex, fleet.build());
    }
  }

  private void applyMoveFleet(
      Star.Builder star,
      Collection<Star> auxStars,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.MOVE_FLEET));
    logHandler.log("- moving fleet");

    Star targetStar = null;
    for (Star s : auxStars) {
      if (s.id.equals(modification.star_id)) {
        targetStar = s;
        break;
      }
    }
    if (targetStar == null) {
      // Not suspicious, the caller made a mistake not the user.
      logHandler.log(String.format(Locale.US,
          "  target star #%d was not included in the auxiliary star list.", modification.star_id));
      return;
    }

    int fleetIndex = findFleetIndex(star, modification.fleet_id);
    if (fleetIndex < 0) {
      // TODO: suspicious!
      logHandler.log("  no fleet with given ID on star.");
      return;
    }
    Fleet fleet = star.fleets.get(fleetIndex);

    if (fleet.state != Fleet.FLEET_STATE.IDLE) {
      // TODO: suspicious?
      logHandler.log("  fleet is not idle, can't move.");
      return;
    }

    Design design = DesignHelper.getDesign(fleet.design_type);
    double distance = StarHelper.distanceBetween(star, targetStar);
    double timeInHours = distance / design.speed_px_per_hour;
    double fuel = design.fuel_cost_per_px * distance * fleet.num_ships;

    int storageIndex = StarHelper.getStorageIndex(star, fleet.empire_id);
    if (storageIndex < 0) {
      // No storage. TODO: some kind of portable fuel?
      logHandler.log("  no storages on this star.");
      return;
    }

    EmpireStorage.Builder empireStorageBuilder = star.empire_stores.get(storageIndex).newBuilder();
    if (empireStorageBuilder.total_energy < fuel) {
      logHandler.log(String.format(Locale.US,
          "  not enough energy for move (%.2f < %.2f)", empireStorageBuilder.total_energy, fuel));
      return;
    }

    logHandler.log(String.format(Locale.US, "  cost=%.2f", fuel));
    star.empire_stores.set(storageIndex, empireStorageBuilder
        .total_energy(empireStorageBuilder.total_energy - (float) fuel)
        .build());
    star.fleets.set(fleetIndex, star.fleets.get(fleetIndex).newBuilder()
        .destination_star_id(targetStar.id)
        .state(Fleet.FLEET_STATE.MOVING)
        .state_start_time(System.currentTimeMillis())
        .eta(System.currentTimeMillis() + (long)(timeInHours * HOURS_MS))
        .build());
  }

  private void applyEmptyNative(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.EMPTY_NATIVE));
    logHandler.log("- emptying native colonies");

    for (int i = 0; i < star.planets.size(); i++) {
      if (star.planets.get(i).colony != null
          && star.planets.get(i).colony.empire_id == null) {
        star.planets.set(i, star.planets.get(i).newBuilder()
            .colony(null)
            .build());
      }
    }

    for (int i = 0; i < star.empire_stores.size(); i++) {
      if (star.empire_stores.get(i).empire_id == null) {
        star.empire_stores.remove(i);
        i--;
      }
    }

    for (int i = 0; i < star.fleets.size(); i++) {
      if (star.fleets.get(i).empire_id == null) {
        star.fleets.remove(i);
        i--;
      }
    }
  }

  @Nullable
  private Planet getPlanetWithColony(Star.Builder star, long colonyId) {
    for (int i = 0; i < star.planets.size(); i++) {
      Planet planet = star.planets.get(i);
      if (planet.colony != null && planet.colony.id.equals(colonyId)) {
        return planet;
      }
    }

    return null;
  }

  private int findFleetIndex(Star.Builder star, long fleetId) {
    for (int i = 0; i < star.fleets.size(); i++) {
      if (star.fleets.get(i).id.equals(fleetId)) {
        return i;
      }
    }
    return -1;
  }
}
