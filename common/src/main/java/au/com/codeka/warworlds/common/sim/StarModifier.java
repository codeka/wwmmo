package au.com.codeka.warworlds.common.sim;

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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Class for handling modifications to a star.
 */
public class StarModifier {
  private static final Log log = new Log("StarModifier");

  private static final long HOURS_MS = 3600000L;

  private static final Simulation.LogHandler EMPTY_LOG_HANDLER = new Simulation.LogHandler() {
    @Override
    public void setStarName(String starName) {
    }

    @Override
    public void log(String message) {
    }
  };

  public interface IdentifierGenerator {
    long nextIdentifier();
  }

  private final IdentifierGenerator identifierGenerator;

  public StarModifier(IdentifierGenerator identifierGenerator) {
    this.identifierGenerator = identifierGenerator;
  }

  /**
   * Modify the given {@link Star.Builder} with the given {@link StarModification}.
   *
   * @param star The star to modify.
   * @param modification The modification to apply.
   * @throws SuspiciousModificationException when the modification seems suspicious, or isn't
   *    otherwise allowed (e.g. you're trying to modify another empire's star, for example).
   */
  public void modifyStar(Star.Builder star, StarModification modification)
      throws SuspiciousModificationException {
    modifyStar(star, null, Lists.newArrayList(modification), null);
  }

  /**
   * Modify the given {@link Star.Builder} with the given {@link StarModification}.
   *
   * @param star The star to modify.
   * @param modification The modification to apply.
   * @param logHandler A {@link Simulation.LogHandler} to write logs to. Can be null if you don't
   *                   want to log.
   * @throws SuspiciousModificationException when the modification seems suspicious, or isn't
   *    otherwise allowed (e.g. you're trying to modify another empire's star, for example).
   */
  public void modifyStar(
      Star.Builder star,
      StarModification modification,
      @Nullable Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
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
   * @throws SuspiciousModificationException when the modification seems suspicious, or isn't
   *    otherwise allowed (e.g. you're trying to modify another empire's star, for example).
   */
  public void modifyStar(
      Star.Builder star,
      @Nullable Collection<Star> auxStars,
      Collection<StarModification> modifications,
      @Nullable Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
    if (logHandler == null) {
      logHandler = EMPTY_LOG_HANDLER;
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
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
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
      case UPGRADE_BUILDING:
        applyUpgradeBuilding(star, modification, logHandler);
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
    checkArgument(modification.type.equals(StarModification.MODIFICATION_TYPE.COLONIZE));
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
            // Make sure we don't have too much fuel.
            Design design = DesignHelper.getDesign(fleet.design_type);
            float maxFuelAmount = design.fuel_size * (fleet.num_ships - 1);
            float fuelAmount = Math.max(fleet.fuel_amount, maxFuelAmount);

            star.fleets.set(i, fleet.newBuilder()
                .num_ships(fleet.num_ships - 1)
                .fuel_amount(fuelAmount)
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
    checkArgument(modification.type.equals(StarModification.MODIFICATION_TYPE.CREATE_FLEET));

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
    float fuelAmount = 0.0f;
    Design design =
        DesignHelper.getDesign(
            modification.fleet == null
                ? modification.design_type
                : modification.fleet.design_type);
    float numShips = modification.fleet == null ? modification.count : modification.fleet.num_ships;
    if (modification.full_fuel != null && modification.full_fuel) {
      fuelAmount = design.fuel_size * numShips;
    } else {
      fuelAmount = design.fuel_size * numShips;
      if (modification.fleet != null) {
        fuelAmount -= modification.fleet.fuel_amount;
      }

      int storageIndex = StarHelper.getStorageIndex(star, modification.empire_id);
      EmpireStorage.Builder empireStorage = star.empire_stores.get(storageIndex).newBuilder();
      fuelAmount = Math.min(fuelAmount, empireStorage.total_energy);
      empireStorage.total_energy(empireStorage.total_energy - fuelAmount);
      star.empire_stores.set(storageIndex, empireStorage.build());
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
          .fuel_amount(fuelAmount)
          .destination_star_id(null)
          .eta(null)
          .build());
    } else {
      star.fleets.add(new Fleet.Builder()
          //TODO: .alliance_id()
          .design_type(modification.design_type)
          .empire_id(modification.empire_id)
          .id(identifierGenerator.nextIdentifier())
          .num_ships((float) modification.count)
          .fuel_amount(fuelAmount)
          .stance(Fleet.FLEET_STANCE.AGGRESSIVE)
          .state(attack ? Fleet.FLEET_STATE.ATTACKING : Fleet.FLEET_STATE.IDLE)
          .state_start_time(System.currentTimeMillis())
          .build());
    }
  }

  private void applyCreateBuilding(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
    checkArgument(modification.type.equals(StarModification.MODIFICATION_TYPE.CREATE_BUILDING));

    Planet planet = getPlanetWithColony(star, modification.colony_id);
    if (planet != null) {
      if (!EmpireHelper.isSameEmpire(planet.colony.empire_id, modification.empire_id)) {
        throw new SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to create building on planet for different empire. colony.empire_id=%d",
            planet.colony.empire_id);
      }

      logHandler.log(
          String.format(Locale.US, "- creating building, colony_id=%d", modification.colony_id));
      Colony.Builder colony = planet.colony.newBuilder();
      colony.buildings.add(new Building.Builder()
          .id(identifierGenerator.nextIdentifier())
          .design_type(modification.design_type)
          .level(1)
          .build());
      star.planets.set(planet.index, planet.newBuilder()
          .colony(colony.build())
          .build());
    } else {
      throw new SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to create building on colony that does not exist. colony_id=%d",
          modification.colony_id);
    }
  }

  private void applyAdjustFocus(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
    checkArgument(modification.type.equals(StarModification.MODIFICATION_TYPE.ADJUST_FOCUS));

    Planet planet = getPlanetWithColony(star, modification.colony_id);
    if (planet != null) {
      if (!EmpireHelper.isSameEmpire(planet.colony.empire_id, modification.empire_id)) {
        throw new SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to adjust focus on planet for different empire. colony.empire_id=%d",
            planet.colony.empire_id);
      }
      logHandler.log("- adjusting focus.");
      star.planets.set(planet.index, planet.newBuilder()
          .colony(planet.colony.newBuilder()
              .focus(modification.focus)
              .build())
          .build());
    } else {
      throw new SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to adjust focus on a colony that does not exist. colony_id=%d",
          modification.colony_id);
    }
  }

  private void applyAddBuildRequest(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
    checkArgument(modification.type.equals(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST));

    Planet planet = getPlanetWithColony(star, modification.colony_id);
    if (planet != null) {
      Colony.Builder colonyBuilder = planet.colony.newBuilder();
      if (!EmpireHelper.isSameEmpire(colonyBuilder.empire_id, modification.empire_id)) {
        throw new SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to add build request on colony that does belong to you. colony_id=%d empire_id=%d",
            modification.colony_id,
            colonyBuilder.empire_id);
      }

      // If the build request is for a ship and this colony doesn't have a shipyard, you can't
      // build. That's suspicious.
      Design design = DesignHelper.getDesign(modification.design_type);
      if (design.design_kind == Design.DesignKind.SHIP) {
        boolean hasShipyard = false;
        for (Building building : colonyBuilder.buildings) {
          if (building.design_type == Design.DesignType.SHIPYARD) {
            hasShipyard = true;
          }
        }
        if (!hasShipyard) {
          throw new SuspiciousModificationException(
              star.id,
              modification,
              "Attempt to build ship with no shipyard present.");
        }
      }

      // If this is not a building, then building_id must be null.
      if (design.design_kind != Design.DesignKind.BUILDING && modification.building_id != null) {
        throw new SuspiciousModificationException(
            star.id,
            modification,
            "Cannot upgrade something that is not a building.");
      }

      // If the build request has a building_id, then that building should be on this colony.
      if (modification.building_id != null) {
        boolean found = false;
        for (Building bldg : colonyBuilder.buildings) {
          if (Objects.equals(bldg.id, modification.building_id)) {
            found = true;
          }
        }
        if (!found) {
          throw new SuspiciousModificationException(
              star.id,
              modification,
              "Cannot upgrade a building that doesn't exist: #%d", modification.building_id);
        }
      }

      int count = 1;
      if (design.design_kind == Design.DesignKind.SHIP) {
        count = modification.count;
      }

      logHandler.log("- adding build request");
      colonyBuilder.build_requests.add(new BuildRequest.Builder()
          .id(identifierGenerator.nextIdentifier())
          .design_type(modification.design_type)
          .building_id(modification.building_id)
          .start_time(System.currentTimeMillis())
          .count(count)
          .progress(0.0f)
          .build());
      star.planets.set(planet.index, planet.newBuilder()
          .colony(colonyBuilder.build())
          .build());
    } else {
      throw new SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to add build request on colony that does not exist. colony_id=%d",
          modification.colony_id);
    }
  }

  private void applyDeleteBuildRequest(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
    checkArgument(modification.type.equals(StarModification.MODIFICATION_TYPE.DELETE_BUILD_REQUEST));

    Planet planet = null;
    BuildRequest buildRequest = null;
    for (Planet p : star.planets) {
      if (p.colony != null) {
        for (BuildRequest br : p.colony.build_requests) {
          if (br.id.equals(modification.build_request_id)) {
            if (!EmpireHelper.isSameEmpire(p.colony.empire_id, modification.empire_id)) {
              throw new SuspiciousModificationException(
                  star.id,
                  modification,
                  "Attempt to delete build request for different empire. building.empire_id=%d",
                  p.colony.empire_id);
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
      throw new SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to delete build request that does not exist. build_request_id=%d",
          modification.build_request_id);
    }
    final long idToDelete = buildRequest.id;

    logHandler.log("- deleting build request");
    Colony.Builder colonyBuilder = planet.colony.newBuilder();
    colonyBuilder.build_requests(
        Lists.newArrayList(
            Iterables.filter(planet.colony.build_requests, br -> !br.id.equals(idToDelete))));
    star.planets.set(planet.index, planet.newBuilder()
        .colony(colonyBuilder.build())
        .build());
  }

  private void applySplitFleet(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
    checkArgument(modification.type.equals(StarModification.MODIFICATION_TYPE.SPLIT_FLEET));

    int fleetIndex = findFleetIndex(star, modification.fleet_id);
    if (fleetIndex >= 0) {
      Fleet.Builder fleet = star.fleets.get(fleetIndex).newBuilder();
      if (!EmpireHelper.isSameEmpire(fleet.empire_id, modification.empire_id)) {
        throw new SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to split fleet of different empire. fleet.empire_id=%d",
            fleet.empire_id);
      }

      Design design = DesignHelper.getDesign(fleet.design_type);
      float fuelFraction= fleet.fuel_amount / (design.fuel_size * fleet.num_ships);

      logHandler.log("- splitting fleet");
      // Modify the existing fleet to change it's number of ships
      float existingFleetNumShips = fleet.num_ships - modification.count;
      star.fleets.set(fleetIndex, fleet
          .num_ships(existingFleetNumShips)
          .fuel_amount(fuelFraction * (design.fuel_size * existingFleetNumShips))
          .build());

      // Add a new fleet, that's a copy of the existing fleet, but with the new number of ships.
      fleet = star.fleets.get(fleetIndex).newBuilder();
      star.fleets.add(fleet
          .id(identifierGenerator.nextIdentifier())
          .num_ships((float) modification.count)
          .fuel_amount(fuelFraction * (design.fuel_size * modification.count))
          .build());
    } else {
      throw new SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to split fleet that does not exist. fleet_id=%d",
          modification.fleet_id);
    }
  }

  private void applyMergeFleet(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
    checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.MERGE_FLEET));

    int fleetIndex = findFleetIndex(star, modification.fleet_id);
    if (fleetIndex >= 0) {
      Fleet.Builder fleet = star.fleets.get(fleetIndex).newBuilder();
      if (fleet.state != Fleet.FLEET_STATE.IDLE) {
        // Can't merge, but this isn't particularly suspicious.
        logHandler.log(String.format(Locale.US,
            "  main fleet %d is %s, cannot merge.", fleet.id, fleet.state));
      }

      if (!EmpireHelper.isSameEmpire(fleet.empire_id, modification.empire_id)) {
        throw new SuspiciousModificationException(
            star.id,
            modification,
            "Attempt to merge fleet owned by a different empire. fleet.empire_id=%d",
            fleet.empire_id);
      }

      for (int i = 0; i < star.fleets.size(); i++) {
        Fleet thisFleet = star.fleets.get(i);
        if (thisFleet.id.equals(fleet.id)) {
          continue;
        }
        if (modification.additional_fleet_ids.contains(thisFleet.id)) {
          if (!thisFleet.design_type.equals(fleet.design_type)) {
            // The client shouldn't allow you to select a fleet of a different type. This would be
            // suspicious.
            throw new SuspiciousModificationException(
                star.id,
                modification,
                "Fleet #%d not the same design_type as #%d (%s vs. %s)",
                thisFleet.id, fleet.id, thisFleet.design_type, fleet.design_type);
          }

          if (!EmpireHelper.isSameEmpire(thisFleet.empire_id, modification.empire_id)) {
            throw new SuspiciousModificationException(
                star.id,
                modification,
                "Attempt to merge fleet owned by a different empire. fleet.empire_id=%d",
                thisFleet.empire_id);
          }

          if (thisFleet.state != Fleet.FLEET_STATE.IDLE) {
            // Again, not particularly suspicious, we'll just skip it.
            logHandler.log(String.format(Locale.US,
                "  fleet %d is %s, cannot merge.", thisFleet.id, thisFleet.state));
            continue;
          }

          // TODO: make sure it has the same upgrades, otherwise we have to remove it.

          fleet.num_ships(fleet.num_ships + thisFleet.num_ships);
          fleet.fuel_amount(fleet.fuel_amount + thisFleet.fuel_amount);
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
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException{
    checkArgument(
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
      throw new SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to move fleet that does not exist. fleet_id=%d",
          modification.fleet_id);
    }
    Fleet fleet = star.fleets.get(fleetIndex);

    if (!fleet.empire_id.equals(modification.empire_id)) {
      throw new SuspiciousModificationException(
          star.id,
          modification,
          "Attempt to move fleet owned by a different empire. fleet.empire_id=%d",
          fleet.empire_id);
    }

    if (fleet.state != Fleet.FLEET_STATE.IDLE) {
      // Not suspicious, maybe you accidentally pressed twice.
      logHandler.log("  fleet is not idle, can't move.");
      return;
    }

    Design design = DesignHelper.getDesign(fleet.design_type);
    double distance = StarHelper.distanceBetween(star, targetStar);
    double timeInHours = distance / design.speed_px_per_hour;
    double fuel = design.fuel_cost_per_px * distance * fleet.num_ships;

    if (fleet.fuel_amount < fuel) {
      // Not enough fuel. We won't count it as suspicious, maybe a race condition.
      logHandler.log(
          String.format(
              Locale.US,
              "  not enough fuel in the fleet (needed %.2f, have %.2f",
              fuel, fleet.fuel_amount));
      return;
    }

    logHandler.log(String.format(Locale.US, "  cost=%.2f", fuel));

    star.fleets.set(fleetIndex, star.fleets.get(fleetIndex).newBuilder()
        .destination_star_id(targetStar.id)
        .state(Fleet.FLEET_STATE.MOVING)
        .fuel_amount(fleet.fuel_amount - (float) fuel)
        .state_start_time(System.currentTimeMillis())
        .eta(System.currentTimeMillis() + (long)(timeInHours * HOURS_MS))
        .build());
  }

  private void applyEmptyNative(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler) {
    checkArgument(
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

  private void applyUpgradeBuilding(
      Star.Builder star,
      StarModification modification,
      Simulation.LogHandler logHandler)
      throws SuspiciousModificationException {
    checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.UPGRADE_BUILDING));
    logHandler.log("- upgrading building");
    for (int i = 0; i < star.planets.size(); i++) {
      if (star.planets.get(i).colony != null
          && star.planets.get(i).colony.id.equals(modification.colony_id)) {
        Colony.Builder colony = star.planets.get(i).colony.newBuilder();
        if (!colony.empire_id.equals(modification.empire_id)) {
          throw new SuspiciousModificationException(
              star.id,
              modification,
              "trying to upgrade building belonging to different empire (%d)", colony.empire_id);
        }

        boolean found = false;
        for (int j = 0; j < colony.buildings.size(); j++) {
          if (colony.buildings.get(j).id.equals(modification.building_id)) {
            found = true;
            Building.Builder building = colony.buildings.get(j).newBuilder();
            // TODO: check if it's at max level?
            building.level(building.level + 1);
            colony.buildings.set(j, building.build());
          }
        }

        if (!found) {
          throw new SuspiciousModificationException(
              star.id,
              modification,
              "trying to upgrade building that doesn't exist (%d)", modification.building_id);
        }

        star.planets.set(i, star.planets.get(i).newBuilder().colony(colony.build()).build());
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
