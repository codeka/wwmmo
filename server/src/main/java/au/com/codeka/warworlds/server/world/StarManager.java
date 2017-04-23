package au.com.codeka.warworlds.server.world;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.NormalRandom;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.common.sim.Simulation;
import au.com.codeka.warworlds.common.sim.StarModifier;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.store.StarsStore;

/**
 * Manages stars and keeps the up-to-date in the data store.
 */
public class StarManager {
  private static final Log log = new Log("StarManager");
  public static final StarManager i = new StarManager();

  private final StarsStore store;
  private final HashMap<Long, WatchableObject<Star>> stars = new HashMap<>();
  private final StarModifier starModifier;

  private StarManager() {
    store = DataStore.i.stars();
    starModifier = new StarModifier(() -> DataStore.i.seq().nextIdentifier());
  }

  public WatchableObject<Star> getStar(long id) {
    WatchableObject<Star> watchableStar;
    synchronized (stars) {
      watchableStar = stars.get(id);
      if (watchableStar == null) {
        Star star = store.get(id);
        if (star == null) {
          return null;
        }

        watchableStar = new WatchableObject<>(star);
        watchableStar.addWatcher(starWatcher);
        stars.put(star.id, watchableStar);
      }
    }

    return watchableStar;
  }

  public void deleteStar(long id) {
    WatchableObject<Star> watchableStar = stars.get(id);
    Star star;
    if (watchableStar != null) {
      star = watchableStar.get();
    } else {
      star = store.get(id);
      if (star == null) {
        // If the star's not in the store, it doesn't exist.
        return;
      }
    }
    SectorCoord coord = new SectorCoord.Builder().x(star.sector_x).y(star.sector_y).build();

    store.delete(id);
    synchronized (stars) {
      stars.remove(id);
    }
    SectorManager.i.forgetSector(coord);
  }

  /**
   * Add native colonies to the star with the given ID. We assume it's already eligible for one.
   */
  public void addNativeColonies(long id) {
    WatchableObject<Star> star = getStar(id);
    synchronized (star.lock) {
      log.debug("Adding native colonies to star %d \"%s\"...", star.get().id, star.get().name);

      // OK, so basically any planet with a population congeniality > 500 will get a colony.
      Star.Builder starBuilder = star.get().newBuilder();

      int numColonies = 0;
      for (int i = 0; i < starBuilder.planets.size(); i++) {
        if (starBuilder.planets.get(i).population_congeniality > 500) {
          starModifier.modifyStar(starBuilder, new StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.COLONIZE)
              .planet_index(i)
              .build());
          numColonies ++;
        }
      }

      // Create a fleet of fighters for each colony.
      NormalRandom rand = new NormalRandom();
      while (numColonies > 0) {
        int numShips = 100 + (int) (rand.next() * 40);
        starModifier.modifyStar(starBuilder, new StarModification.Builder()
            .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
            .design_type(Design.DesignType.FIGHTER)
            .count(numShips)
            .build());

        numColonies--;
      }

      star.set(starBuilder.build());
    }
  }

  public ArrayList<WatchableObject<Star>> getStarsForEmpire(long empireId) {
    ArrayList<WatchableObject<Star>> stars = new ArrayList<>();
    for (Long id : store.getStarsForEmpire(empireId)) {
      stars.add(getStar(id));
    }
    return stars;
  }

  public void modifyStar(
      WatchableObject<Star> star,
      Collection<StarModification> modifications,
      @Nullable Simulation.LogHandler logHandler) {
    Map<Long, Star> auxStars = null;
    for (StarModification modification : modifications) {
      if (modification.star_id != null) {
        auxStars = auxStars == null ? new TreeMap<>() : auxStars;
        if (!auxStars.containsKey(modification.star_id)) {
          Star auxStar = getStar(modification.star_id).get();
          auxStars.put(auxStar.id, auxStar);
        }
      }
    }

    modifyStar(star, auxStars == null ? null : auxStars.values(), modifications, logHandler);
  }

  private void modifyStar(
      WatchableObject<Star> star,
      @Nullable Collection<Star> auxStars,
      Collection<StarModification> modifications,
      @Nullable Simulation.LogHandler logHandler) {
    synchronized (star.lock) {
      Star.Builder starBuilder = star.get().newBuilder();
      starModifier.modifyStar(starBuilder, auxStars, modifications, logHandler);
      completeActions(star, starBuilder, logHandler);
    }
  }

  /**
   * Call this after simulating a star to complete the actions required (e.g. if a building has
   * finished or a fleet has arrived) and also save the star to the data store.
   *
   * @param star The {@link WatchableObject<Star>} of the star that we'll update.
   * @param starBuilder A simulated star that we need to finish up.
   * @param logHandler An optional {@link Simulation.LogHandler} that we'll pass log messages
   *                   through to. If null, we'll just do normal logging.
   */
  private void completeActions(
      WatchableObject<Star> star,
      Star.Builder starBuilder,
      @Nullable Simulation.LogHandler logHandler) {
    // For any builds/moves/etc that finish in the future, make sure we schedule a job to
    // re-simulate the star then.
    Long nextSimulateTime = null;

    // TODO: pass this into modifyStar as well so the simulation uses the same time everywhere.
    long now = System.currentTimeMillis();

    // Any builds which have finished, we'll want to remove them and add modifications for them
    // instead.
    for (int i = 0; i < starBuilder.planets.size(); i++) {
      Planet planet = starBuilder.planets.get(i);
      if (planet.colony == null || planet.colony.build_requests == null) {
        continue;
      }

      ArrayList<BuildRequest> remainingBuildRequests = new ArrayList<>();
      for (BuildRequest br : planet.colony.build_requests) {
        if (br.end_time <= now) {
          Design design = DesignHelper.getDesign(br.design_type);
          if (design.design_kind == Design.DesignKind.BUILDING) {
            starModifier.modifyStar(
                starBuilder,
                null,
                Lists.newArrayList(new StarModification.Builder()
                    .type(StarModification.MODIFICATION_TYPE.CREATE_BUILDING)
                    .colony_id(planet.colony.id)
                    .empire_id(planet.colony.empire_id)
                    .design_type(br.design_type)
                    .build()),
                logHandler);
          } else {
            starModifier.modifyStar(
                starBuilder,
                null,
                Lists.newArrayList(new StarModification.Builder()
                    .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
                    .empire_id(planet.colony.empire_id)
                    .design_type(br.design_type)
                    .count(br.count)
                    .build()),
                logHandler);
          }
          // TODO: add a sitrep as well
        } else {
          if (nextSimulateTime == null || nextSimulateTime > br.end_time) {
            nextSimulateTime = br.end_time;
          }
          remainingBuildRequests.add(br);
        }
      }

      Planet.Builder planetBuilder = starBuilder.planets.get(i).newBuilder();
      planetBuilder.colony(planetBuilder.colony.newBuilder()
          .build_requests(remainingBuildRequests)
          .build());
      starBuilder.planets.set(i, planetBuilder.build());
    }

    // Any fleets that have arrived, make sure we remove them here and add them to the destination.
    for (int i = 0; i < starBuilder.fleets.size(); i++) {
      Fleet fleet = starBuilder.fleets.get(i);
      if (fleet.state != Fleet.FLEET_STATE.MOVING || fleet.eta > now) {
        continue;
      }

      // First, grab the destination star and add it there.
      WatchableObject<Star> destStar = getStar(fleet.destination_star_id);
      synchronized (destStar.lock) { // TODO: this could deadlock, need to lock in the same order
        Star.Builder destStarBuilder = destStar.get().newBuilder();
        starModifier.modifyStar(
            destStarBuilder,
            null,
            Lists.newArrayList(new StarModification.Builder()
                .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
                .empire_id(fleet.empire_id)
                .fleet(fleet)
                .build()),
            logHandler);
        destStar.set(destStarBuilder.build());
      }

      // Then remove it from our star.
      starBuilder.fleets.remove(i);
    }

    // Any fleets that have been destroyed, destroy them.
    for (int i = 0; i < starBuilder.fleets.size(); i++) {
      Fleet fleet = starBuilder.fleets.get(i);
      if (fleet.num_ships <= 0.01f) {
        starBuilder.fleets.remove(i);
        i--;
      }

      // TODO: add a sitrep?
    }

    // Make sure we simulate at least when the next fleet arrives
    for (Fleet fleet : starBuilder.fleets) {
      if (fleet.eta != null && (nextSimulateTime == null || nextSimulateTime > fleet.eta)) {
        nextSimulateTime = fleet.eta;
      }
    }

    starBuilder.next_simulation(nextSimulateTime);
    star.set(starBuilder.build());

    // TODO: only ping if the next simulate time is in the next 10 minutes.
    StarSimulatorQueue.i.ping();
  }

  private final WatchableObject.Watcher<Star> starWatcher = new WatchableObject.Watcher<Star>() {
    @Override
    public void onUpdate(WatchableObject<Star> star) {
      log.debug("Saving star %d %s", star.get().id, star.get().name);
      store.put(star.get().id, star.get());
    }
  };
}
