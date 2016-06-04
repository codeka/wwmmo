package au.com.codeka.warworlds.server.world;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;

import java.util.Collection;
import java.util.HashMap;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.ColonyFocus;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.Simulation;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.store.ProtobufStore;

/**
 * Manages stars and keeps the up-to-date in the data store.
 */
public class StarManager {
  private static final Log log = new Log("StarManager");
  public static final StarManager i = new StarManager();

  private final ProtobufStore<Star> store;
  private final HashMap<Long, WatchableObject<Star>> stars = new HashMap<>();

  private StarManager() {
    store = DataStore.i.stars();
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

  public void modifyStar(WatchableObject<Star> star, StarModification modification) {
    modifyStar(star, Lists.newArrayList(modification));
  }

  public void modifyStar(WatchableObject<Star> star, Collection<StarModification> modifications) {
    synchronized (star.lock) {
      for (StarModification modification : modifications) {
        applyModification(star, modification);
      }
    }
  }

  private void applyModification(WatchableObject<Star> star, StarModification modification) {
    switch (modification.type) {
      case COLONIZE:
        applyColonize(star, modification);
        return;
      case CREATE_FLEET:
        applyCreateFleet(star, modification);
        return;
      default:
        log.error("Unknown or unexpected modification type: %s", modification.type);
    }
  }

  private void applyColonize(WatchableObject<Star> star, StarModification modification) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.COLONIZE));

    Star.Builder starBuilder = star.get().newBuilder();
    new Simulation().simulate(starBuilder);
    starBuilder.planets.set(
        modification.planet_index,
        starBuilder.planets.get(modification.planet_index).newBuilder()
            .colony(new Colony.Builder()
                .cooldown_end_time(DateTime.now().plusMinutes(15).getMillis())
                .empire_id(modification.empire_id)
                .focus(new ColonyFocus.Builder()
                    .construction(0.1f)
                    .energy(0.3f)
                    .farming(0.3f)
                    .mining(0.3f)
                    .build())
                .id(store.nextIdentifier())
                .population(100.0f)
                .defence_bonus(1.0f)
                .build())
            .build());

    // if there's no storage for this empire, add one with some defaults now.
    boolean hasStorage = false;
    for (EmpireStorage storage : starBuilder.empire_stores) {
      if (storage.empire_id != null && storage.empire_id.equals(modification.empire_id)) {
        hasStorage = true;
      }
    }
    if (!hasStorage) {
      starBuilder.empire_stores.add(new EmpireStorage.Builder()
          .empire_id(modification.empire_id)
          .total_goods(100.0f).total_minerals(100.0f).total_energy(1000.0f)
          .max_goods(1000.0f).max_minerals(1000.0f).max_energy(1000.0f)
          .build());
    }

    star.set(starBuilder.build());
  }

  private void applyCreateFleet(WatchableObject<Star> star, StarModification modification) {
    Preconditions.checkArgument(
        modification.type.equals(StarModification.MODIFICATION_TYPE.CREATE_FLEET));

    // TODO: simulate star
    Star.Builder starBuilder = star.get().newBuilder();
    starBuilder.fleets.add(new Fleet.Builder()
        //TODO: .alliance_id()
        .design_id(modification.design_id)
        .empire_id(modification.empire_id)
        .id(store.nextIdentifier())
        .num_ships((float) modification.count)
        .stance(Fleet.FLEET_STANCE.AGGRESSIVE)
        .state(Fleet.FLEET_STATE.IDLE)
        .state_start_time(DateTime.now().getMillis())
        .build());
    star.set(starBuilder.build());
  }

  private final WatchableObject.Watcher<Star> starWatcher = new WatchableObject.Watcher<Star>() {
    @Override
    public void onUpdate(WatchableObject<Star> star) {
      store.put(star.get().id, star.get());
    }
  };
}
