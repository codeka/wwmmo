package au.com.codeka.warworlds.model;

import android.support.v4.util.LruCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.eventbus.EventBus;
import au.com.codeka.warworlds.eventbus.EventHandler;

/**
 * This class "manages" the list of \c StarfieldSector's that we have loaded
 * and is responsible for loading new sectors and freeing old ones as we
 * scroll around.
 * <p/>
 * The way we manage the scrolling position is we have a "sectorX, sectorY"
 * which determines which sector is in the centre of the view, then we have
 * an "offsetX, offsetY" which is a pixel offset to apply when drawing the
 * sectors (so you can smoothly scroll, of course).
 */
public class SectorManager extends BaseManager {
  public static final SectorManager i = new SectorManager();
  public static final EventBus eventBus = new EventBus();

  private final SectorCache sectors = new SectorCache();
  private final Set<Pair<Long, Long>> pendingSectors = new TreeSet<>();
  private final Map<String, Star> sectorStars = new TreeMap<>();

  private SectorManager() {
    StarManager.eventBus.register(eventHandler);
  }

  public void clearCache() {
    sectorStars.clear();
    sectors.clear();
  }

  /**
   * Gets the second with the given x/y coordinates. If the sector is not cached, we DO NOT fetch
   * it.
   */
  public Sector getSector(long sectorX, long sectorY) {
    Pair<Long, Long> key = new Pair<>(sectorX, sectorY);
    return sectors.get(key);
  }

  /** Finds the star with the given key. */
  public Star findStar(String starKey) {
    return sectorStars.get(starKey);
  }

  /** Gets a collection of all visible stars. This is "pretty" big... */
  public Collection<Star> getAllVisibleStars() {
    return sectorStars.values();
  }

  /**
   * Forces us to refresh the given sector, even if we already have it loaded. Useful when
   * we know it's been modified (by our own actions, for example).
   */
  public void refreshSector(long sectorX, long sectorY) {
    ArrayList<Pair<Long, Long>> coords = new ArrayList<>();
    coords.add(new Pair<>(sectorX, sectorY));
    refreshSectors(coords, true);
  }

  /** Fetches the details of a bunch of sectors from the server. */
  public void refreshSectors(final List<Pair<Long, Long>> coords, boolean force) {
    final List<Pair<Long, Long>> missingSectors = new ArrayList<>();
    synchronized (this) {
      for (Pair<Long, Long> coord : coords) {
        Sector s = sectors.get(coord);
        if ((s == null  ||force) && !pendingSectors.contains(coord)) {
          pendingSectors.add(coord);
          missingSectors.add(coord);
        }
      }

      if (!missingSectors.isEmpty()) {
        String url = "";
        for (Pair<Long, Long> coord : missingSectors) {
          if (url.length() != 0) {
            url += "%7C"; // Java doesn't like "|" for some reason (it's valid!!)
          }
          url += String.format("%d,%d", coord.one, coord.two);
        }
        url = "sectors?coords=" + url;

        ApiRequest request = new ApiRequest.Builder(url, "GET")
            .completeCallback(new ApiRequest.CompleteCallback() {
              @Override
              public void onRequestComplete(ApiRequest request) {
                Messages.Sectors sectorsPb = request.body(Messages.Sectors.class);
                for (Messages.Sector sector_pb : sectorsPb.getSectorsList()) {
                  Sector sector = new Sector();
                  sector.fromProtocolBuffer(sector_pb);

                  Pair<Long, Long> key = new Pair<>(sector.getX(), sector.getY());
                  sectors.put(key, sector);
                  eventBus.publish(sector);
                  pendingSectors.remove(key);

                  for (BaseStar baseStar : sector.getStars()) {
                    Star star = (Star) baseStar;
                    sectorStars.put(star.getKey(), star);
                  }
                }
                eventBus.publish(new SectorListChangedEvent());
              }
            }).build();
        RequestManager.i.sendRequest(request);
      }
    }
  }

  /**
   * When we get a notification that a star has been updated, we may need to refresh the whole
   * sector. But we only need to do that if something 'important' about the star has changed (i.e.
   * something actually visible on the starfield view) such as a name change. We don't care if the
   * buildings have changed, for instance.
   */
  private boolean needsUpdate(Star oldStart, Star newStar) {
    if (!oldStart.getName().equals(newStar.getName())) {
      return true;
    }

    for (BaseFleet lhsBaseFleet : oldStart.getFleets()) {
      Fleet lhsFleet = (Fleet) lhsBaseFleet;
      Fleet rhsFleet = (Fleet) newStar.getFleet(Integer.parseInt(lhsFleet.getKey()));
      if (rhsFleet == null) {
        return true;
      }

      if (lhsFleet.getState() != rhsFleet.getState()) {
        return true;
      }
    }

    for (BaseFleet rhsBaseFleet : newStar.getFleets()) {
      Fleet rhsFleet = (Fleet) rhsBaseFleet;
      Fleet lhsFleet = (Fleet) oldStart.getFleet(Integer.parseInt(rhsFleet.getKey()));
      if (lhsFleet == null) {
        return true;
      }
    }

    return false;
  }

  private static class SectorCache implements RealmManager.RealmChangedHandler {
    LruCache<String, Sector> sectors;

    public SectorCache() {
      sectors = new LruCache<>(30);

      RealmManager.i.addRealmChangedHandler(this);
    }

    public void clear() {
      sectors.evictAll();
    }

    public static String key(Pair<Long, Long> coord) {
      return String.format(Locale.ENGLISH, "%d:%d", coord.one, coord.two);
    }

    public Sector get(Pair<Long, Long> coords) {
      return sectors.get(key(coords));
    }

    public void put(Pair<Long, Long> coords, Sector s) {
      sectors.put(key(coords), s);
    }

    /** When we switch realms, we'll want to clear out the cache. */
    @Override
    public void onRealmChanged(Realm newRealm) {
      clear();
    }
  }

  private final Object eventHandler = new Object() {
    @EventHandler(thread = EventHandler.ANY_THREAD)
    public void onStarUpdated(Star star) {
      Star ourStar = findStar(star.getKey());
      if (ourStar != null) {
        if (needsUpdate(ourStar, star)) {
          sectorStars.put(star.getKey(), star);
          Pair<Long, Long> coord = new Pair<>(star.getSectorX(), star.getSectorY());
          Sector sector = sectors.get(coord);
          if (sector != null) {
            for (int i = 0; i < sector.getStars().size(); i++) {
              if (sector.getStars().get(i).getKey().equals(star.getKey())) {
                sector.getStars().set(i, star);
                eventBus.publish(sector);
                return;
              }
            }
          }
        }
      }
    }
  };

  public static class SectorListChangedEvent {
  }
}
