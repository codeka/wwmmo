package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.support.v4.util.LruCache;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.eventbus.EventBus;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;

/**
 * This class "manages" the list of \c StarfieldSector's that we have loaded
 * and is responsible for loading new sectors and freeing old ones as we
 * scroll around.
 * 
 * The way we manage the scrolling position is we have a "sectorX, sectorY"
 * which determines which sector is in the centre of the view, then we have
 * an "offsetX, offsetY" which is a pixel offset to apply when drawing the
 * sectors (so you can smoothly scroll, of course).
 */
public class SectorManager extends BaseManager {
    private static final Log log = new Log("SectorManager");
    public static SectorManager i = new SectorManager();

    public static EventBus eventBus = new EventBus();

    private final SectorCache sectors = new SectorCache();
    private final Set<Pair<Long, Long>> pendingSectors = new TreeSet<Pair<Long, Long>>();
    private final Map<String, Star> sectorStars = new TreeMap<String, Star>();

    private SectorManager() {
        StarManager.eventBus.register(eventHandler);
    }

    private final Object eventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(Star star) {
            Star ourStar = findStar(star.getKey());
            if (ourStar != null) {
                if (!areStarsSame(ourStar, star)) {
                    sectorStars.put(star.getKey(), star);
                    Pair<Long, Long> coord = new Pair<Long, Long>(
                            star.getSectorX(), star.getSectorY());
                    Sector sector = sectors.get(coord);
                    if (sector != null) {
                        for (int i = 0; i < sector.getStars().size(); i++) {
                            if (sector.getStars().get(i).getKey().equals(star.getKey())) {
                                sector.getStars().set(i, star);
                                eventBus.publish(sector);
                            }
                        }
                    }
                }
            }
        }
    };

    public void clearCache() {
        sectorStars.clear();
        sectors.clear();
    }

    public Sector getSector(long sectorX, long sectorY) {
        Pair<Long, Long> key = new Pair<Long, Long>(sectorX, sectorY);
        return sectors.get(key);
    }

    public StarfieldBackgroundRenderer getBackgroundRenderer(Sector s) {
        Pair<Long, Long> coords = new Pair<Long, Long>(s.getX(), s.getY());
        return sectors.getBackgroundRenderer(coords);
    }

    public Collection<Sector> getSectors() {
        return sectors.getAllSectors();
    }

    /**
     * Finds the star with the given key.
     */
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
        ArrayList<Pair<Long, Long>> coords = new ArrayList<Pair<Long, Long>>();
        coords.add(new Pair<Long, Long>(sectorX, sectorY));
        refreshSectors(coords, true);
    }

    /**
     * Fetches the details of a bunch of sectors from the server.
     */
    public void refreshSectors(final List<Pair<Long, Long>> coords, boolean force) {
        Map<Pair<Long, Long>, Sector> existingSectors = new TreeMap<Pair<Long, Long>, Sector>();
        final List<Pair<Long, Long>> missingSectors = new ArrayList<Pair<Long, Long>>();
        synchronized(this) {
            for (Pair<Long, Long> coord : coords) {
                Sector s = sectors.get(coord);
                if (s != null && !force) {
                    existingSectors.put(coord, s);
                } else if (!pendingSectors.contains(coord)) {
                    pendingSectors.add(coord);
                    missingSectors.add(coord);
                }
            }

            if (!missingSectors.isEmpty()) {
                new BackgroundRunner<List<Sector>>() {
                    @Override
                    protected List<Sector> doInBackground() {
                        List<Sector> sectors = null;

                        String url = "";
                        for(Pair<Long, Long> coord : missingSectors) {
                            if (url.length() != 0) {
                                url += "%7C"; // Java doesn't like "|" for some reason (it's valid!!)
                            }
                            url += String.format("%d,%d", coord.one, coord.two);
                        }
                        url = "sectors?coords="+url;
                        try {
                            Messages.Sectors pb = ApiClient.getProtoBuf(url, Messages.Sectors.class);
                            sectors = new ArrayList<Sector>();
                            for (Messages.Sector sector_pb : pb.getSectorsList()) {
                                Sector sector = new Sector();
                                sector.fromProtocolBuffer(sector_pb);
                                sectors.add(sector);
                            }
                        } catch(Exception e) {
                            log.error("Uh Oh!", e);
                        }

                        return sectors;
                    }

                    @Override
                    protected void onComplete(List<Sector> sectors) {
                        if (sectors != null) for(Sector s : sectors) {
                            Pair<Long, Long> key = new Pair<Long, Long>(s.getX(), s.getY());

                            SectorManager.this.sectors.put(key, s);

                            for (BaseStar star : s.getStars()) {
                                sectorStars.put(star.getKey(), (Star) star);
                            }

                            eventBus.publish(s);
                        }

                        for (Pair<Long, Long> coord : missingSectors) {
                            pendingSectors.remove(coord);
                        }

                        eventBus.publish(new SectorListChangedEvent());
                    }
                }.execute();
            }
        }
    }

    /**
     * Determines whether the two stars are the "same" for our purposes. They're only different
     * if they have a new name, or if a fleet has gone from moving->idle or idle->moving.
     */
    private boolean areStarsSame(Star lhs, Star rhs) {
        if (!lhs.getName().equals(rhs.getName())) {
            return false;
        }

        for (BaseFleet lhsBaseFleet : lhs.getFleets()) {
            Fleet lhsFleet = (Fleet) lhsBaseFleet;
            Fleet rhsFleet = (Fleet) rhs.getFleet(Integer.parseInt(lhsFleet.getKey()));
            if (rhsFleet == null) {
                return false;
            }

            if (lhsFleet.getState() != rhsFleet.getState()) {
                return false;
            }
        }

        for (BaseFleet rhsBaseFleet : rhs.getFleets()) {
            Fleet rhsFleet = (Fleet) rhsBaseFleet;
            Fleet lhsFleet = (Fleet) lhs.getFleet(Integer.parseInt(rhsFleet.getKey()));
            if (lhsFleet == null) {
                return false;
            }
        }

        return true;
    }

    private static class SectorCache implements RealmManager.RealmChangedHandler {
        BackgroundRendererCache mBackgroundRenderers;
        LruCache<String, Sector> mSectors;

        public SectorCache() {
            mSectors = new LruCache<String, Sector>(30);
            mBackgroundRenderers = new BackgroundRendererCache();

            RealmManager.i.addRealmChangedHandler(this);
        }

        public void clear() {
            mSectors.evictAll();
            mBackgroundRenderers.evictAll();
        }

        public static String key(Pair<Long, Long> coord) {
            return String.format(Locale.ENGLISH, "%d:%d", coord.one, coord.two);
        }

        public StarfieldBackgroundRenderer getBackgroundRenderer(Pair<Long, Long> coords) {
            String key = key(coords);
            StarfieldBackgroundRenderer renderer = mBackgroundRenderers.get(key);
            if (renderer == null) {
                if (mSectors.get(key) != null) {
                    long[] seeds = new long[9];
                    for (int y = -1; y <= 1; y++) {
                        for (int x = -1; x <= 1; x++) {
                            int n = ((y + 1) * 3) + (x + 1);
                            seeds[n] = (coords.one + x) ^ (coords.two + y) + (coords.one + x);
                        }
                    }
                    renderer = new StarfieldBackgroundRenderer(seeds);
                    mBackgroundRenderers.put(key, renderer);
                }
            }
            return renderer;
        }

        public Sector get(Pair<Long, Long> coords) {
            return mSectors.get(key(coords));
        }

        public void put(Pair<Long, Long> coords, Sector s) {
            mSectors.put(key(coords), s);
        }

        public Collection<Sector> getAllSectors() {
            return mSectors.snapshot().values();
        }

        private class BackgroundRendererCache extends LruCache<String, StarfieldBackgroundRenderer> {
            public BackgroundRendererCache() {
                super(9);
            }

            @Override
            protected void entryRemoved(boolean evicted, String key,
                                        StarfieldBackgroundRenderer oldValue,
                                        StarfieldBackgroundRenderer newValue) {
                if (oldValue != null) {
                    // by explicitly calling close, we free the memory before the GC has to run,
                    // which seems to be a little more robust than just relying on GC...
                    oldValue.close();
                }
            }
        }

        /**
         * When we switch realms, we'll want to clear out the cache.
         */
        @Override
        public void onRealmChanged(Realm newRealm) {
            clear();
        }
    }

    public static class SectorListChangedEvent {
    }
}
