package au.com.codeka.warworlds.server.world;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.store.ProtobufStore;
import au.com.codeka.warworlds.server.world.generator.NewStarFinder;

/**
 * Manages empires, keeps them loaded and ensure they get saved to the data store at the right time.
 */
public class EmpireManager {
  public static final EmpireManager i = new EmpireManager();

  private ProtobufStore<Empire> empires;
  private final Map<Long, WatchableObject<Empire>> watchedEmpires;

  private EmpireManager() {
    empires = DataStore.i.empires();
    watchedEmpires = new HashMap<>();
  }

  public WatchableObject<Empire> getEmpire(long id) {
    synchronized (watchedEmpires) {
      WatchableObject<Empire> watchableEmpire = watchedEmpires.get(id);
      if (watchableEmpire == null) {
        watchableEmpire = watchEmpire(empires.get(id));
      }
      return watchableEmpire;
    }
  }

  /**
   * Create a new {@link Empire}, and return it as a {@link WatchableObject}.
   * @param name The name to give this new empire. We assume you've already confirmed that the name
   *             is unique.
   *
   * @return The new empire, or null if there was an error creating the empire.
   */
  @Nullable
  public WatchableObject<Empire> createEmpire(String name) {
    NewStarFinder newStarFinder = new NewStarFinder();
    if (!newStarFinder.findStarForNewEmpire()) {
      return null;
    }

    return createEmpire(name, newStarFinder);
  }

  /**
   * Create a new {@link Empire}, and return it as a {@link WatchableObject}.
   * @param name The name to give this new empire. We assume you've already confirmed that the name
   *             is unique.
   * @param newStarFinder A {@link NewStarFinder} that's already found the star we will put the
   *                      empire on.
   *
   * @return The new empire, or null if there was an error creating the empire.
   */
  @Nullable
  public WatchableObject<Empire> createEmpire(String name, NewStarFinder newStarFinder) {
    long id = empires.nextIdentifier();

    WatchableObject<Star> star = StarManager.i.getStar(newStarFinder.getStar().id);
    StarManager.i.modifyStar(star, Lists.newArrayList(
        new StarModification.Builder()
            .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
            .empire_id(id)
            .design_type(Design.DesignType.COLONY_SHIP)
            .count(3) // note: one is destroyed by COLONIZE below
            .build(),
        new StarModification.Builder()
            .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
            .empire_id(id)
            .design_type(Design.DesignType.FIGHTER)
            .count(50)
            .build(),
        new StarModification.Builder()
            .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
            .empire_id(id)
            .design_type(Design.DesignType.TROOP_CARRIER)
            .count(200)
            .build(),
        new StarModification.Builder()
            .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
            .empire_id(id)
            .design_type(Design.DesignType.SCOUT)
            .count(10)
            .build(),
        new StarModification.Builder()
            .empire_id(id)
            .type(StarModification.MODIFICATION_TYPE.COLONIZE)
            .planet_index(newStarFinder.getPlanetIndex())
            .build()
    ), null /* logHandler */);

    Empire empire = new Empire.Builder()
        .display_name(name)
        .id(id)
        .home_star(newStarFinder.getStar())
        .build();
    empires.put(id, empire);

    DataStore.i.sectors().removeEmptySector(
        new SectorCoord.Builder().x(star.get().sector_x).y(star.get().sector_y).build());

    return watchEmpire(empire);
  }

  private WatchableObject<Empire> watchEmpire(Empire empire) {
    WatchableObject<Empire> watchableEmpire;
    synchronized (watchedEmpires) {
      watchableEmpire = watchedEmpires.get(empire.id);
      if (watchableEmpire != null) {
        watchableEmpire.set(empire);
      } else {
        watchableEmpire = new WatchableObject<>(empire);
        watchedEmpires.put(watchableEmpire.get().id, watchableEmpire);
      }
    }
    return watchableEmpire;
  }
}
