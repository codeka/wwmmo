package au.com.codeka.warworlds.server.world;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.HashMap;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.StarModifier;
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
  private final StarModifier starModifier;

  private StarManager() {
    store = DataStore.i.stars();
    starModifier = new StarModifier(store::nextIdentifier);
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
      Star.Builder starBuilder = star.get().newBuilder();
      for (StarModification modification : modifications) {
        starModifier.modifyStar(starBuilder, modification);
      }
      star.set(starBuilder.build());
    }
  }

  private final WatchableObject.Watcher<Star> starWatcher = new WatchableObject.Watcher<Star>() {
    @Override
    public void onUpdate(WatchableObject<Star> star) {
      store.put(star.get().id, star.get());
    }
  };
}
