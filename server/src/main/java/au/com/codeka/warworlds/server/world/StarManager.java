package au.com.codeka.warworlds.server.world;

import java.util.HashMap;

import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Manages stars and keeps the up-to-date in the data store.
 */
public class StarManager {
  public static final StarManager i = new StarManager();

  private final HashMap<Long, WatchableObject<Star>> stars = new HashMap<>();

  public WatchableObject<Star> getStar(long id) {
    WatchableObject<Star> watchableStar;
    synchronized (stars) {
      watchableStar = stars.get(id);
      if (watchableStar == null) {
        Star star = DataStore.i.stars().get(id);
        if (star == null) {
          return null;
        }

        watchableStar = new WatchableObject<>(star);
        // TODO: watch it and keep the store updated.
        stars.put(star.id, watchableStar);
      }
    }

    return watchableStar;
  }
}
