package au.com.codeka.warworlds.server.world;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException;
import au.com.codeka.warworlds.server.proto.PatreonInfo;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.store.SectorsStore;
import au.com.codeka.warworlds.server.world.generator.NewStarFinder;
import com.patreon.PatreonAPI;
import com.patreon.resources.Pledge;
import com.patreon.resources.User;

/**
 * Manages empires, keeps them loaded and ensure they get saved to the data store at the right time.
 */
public class EmpireManager {
  private static final Log log = new Log("EmpireManager");
  public static final EmpireManager i = new EmpireManager();

  private final Map<Long, WatchableObject<Empire>> watchedEmpires;

  private EmpireManager() {
    watchedEmpires = new HashMap<>();
  }

  @Nullable
  public WatchableObject<Empire> getEmpire(long id) {
    synchronized (watchedEmpires) {
      WatchableObject<Empire> watchableEmpire = watchedEmpires.get(id);
      if (watchableEmpire == null) {
        Empire empire = DataStore.i.empires().get(id);
        if (empire == null) {
          return null;
        }
        watchableEmpire = watchEmpire(empire);
      }
      return watchableEmpire;
    }
  }

  /**
   * Searches the database for empires matching the given query string.
   */
  public List<WatchableObject<Empire>> search(String query) {
    List<Long> empireIds = DataStore.i.empires().search(query);
    List<WatchableObject<Empire>> empires = new ArrayList<>();
    for (Long id : empireIds) {
      empires.add(getEmpire(id));
    }
    return empires;
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
    log.info("Creating new empire %s", name);
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
    long id = DataStore.i.seq().nextIdentifier();

    WatchableObject<Star> star = StarManager.i.getStar(newStarFinder.getStar().id);
    if (star == null) {
      // Shouldn't happen, NewStarFinder should actually find a star.
      log.error("Unknown star?");
      return null;
    }
    try {
      StarManager.i.modifyStar(star, Lists.newArrayList(
          new StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.EMPTY_NATIVE)
              .build(),
          new StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
              .empire_id(id)
              .design_type(Design.DesignType.COLONY_SHIP)
              .count(3) // note: one is destroyed by COLONIZE below
              .full_fuel(true)
              .build(),
          new StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
              .empire_id(id)
              .design_type(Design.DesignType.FIGHTER)
              .count(50)
              .full_fuel(true)
              .build(),
          new StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
              .empire_id(id)
              .design_type(Design.DesignType.TROOP_CARRIER)
              .count(200)
              .full_fuel(true)
              .build(),
          new StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.CREATE_FLEET)
              .empire_id(id)
              .design_type(Design.DesignType.SCOUT)
              .count(10)
              .full_fuel(true)
              .build(),
          new StarModification.Builder()
              .empire_id(id)
              .type(StarModification.MODIFICATION_TYPE.COLONIZE)
              .planet_index(newStarFinder.getPlanetIndex())
              .build()
      ), null /* logHandler */);
    } catch (SuspiciousModificationException e) {
      // Shouldn't happen, none of these should be suspicious...
      log.error("Unexpected suspicious modification.", e);
      return null;
    }

    Empire empire = new Empire.Builder()
        .display_name(name)
        .id(id)
        .home_star(newStarFinder.getStar())
        .build();
    DataStore.i.empires().put(id, empire);

    DataStore.i.sectors().updateSectorState(
        new SectorCoord.Builder().x(star.get().sector_x).y(star.get().sector_y).build(),
        SectorsStore.SectorState.NonEmpty);

    return watchEmpire(empire);
  }

  /**
   * Refreshes the Patreon data for the given {@link Empire}, by making a request to Patreon's
   * server.
   */
  public void refreshPatreonInfo(
      WatchableObject<Empire> empire, PatreonInfo patreonInfo) throws IOException {
    log.info("Refreshing Patreon pledges for %d (%s).",
        empire.get().id, empire.get().display_name);

    PatreonAPI apiClient = new PatreonAPI(patreonInfo.access_token);
    JSONAPIDocument<User> userJson = apiClient.fetchUser();
    User user = userJson.get();

    int maxPledge = 0;
    if (user.getPledges() != null) {
      for (Pledge pledge : user.getPledges()) {
        if (pledge.getAmountCents() > maxPledge) {
          maxPledge = pledge.getAmountCents();
        }
      }
    }

    patreonInfo = patreonInfo.newBuilder()
        .full_name(user.getFullName())
        .about(user.getAbout())
        .discord_id(user.getDiscordId())
        .patreon_url(user.getUrl())
        .email(user.getEmail())
        .image_url(user.getImageUrl())
        .max_pledge(maxPledge)
        .build();
    DataStore.i.empires().savePatreonInfo(empire.get().id, patreonInfo);
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
