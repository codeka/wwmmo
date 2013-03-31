package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.support.v4.util.LruCache;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.Pair;
import au.com.codeka.Point2D;
import au.com.codeka.common.Vector2;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;
import au.com.codeka.warworlds.model.protobuf.Messages;

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
public class SectorManager {
    private static Logger log = LoggerFactory.getLogger(SectorManager.class);
    private static SectorManager sInstance;

    public static SectorManager getInstance() {
        if (sInstance == null) {
            sInstance = new SectorManager();
        }
        return sInstance;
    }

    private SectorCache mSectors;
    private Map<Pair<Long, Long>, List<OnSectorsFetchedListener>> mInTransitListeners;
    private CopyOnWriteArrayList<OnSectorListChangedListener> mSectorListChangedListeners;
    private Map<String, Star> mSectorStars;

    public static int SECTOR_SIZE = 1024;

    private SectorManager() {
        mSectors = new SectorCache();
        mInTransitListeners = new TreeMap<Pair<Long, Long>, List<OnSectorsFetchedListener>>();
        mSectorListChangedListeners = new CopyOnWriteArrayList<OnSectorListChangedListener>();
        mSectorStars = new TreeMap<String, Star>();
    }

    public Sector getSector(long sectorX, long sectorY) {
        Pair<Long, Long> key = new Pair<Long, Long>(sectorX, sectorY);
        return mSectors.get(key);
    }

    public StarfieldBackgroundRenderer getBackgroundRenderer(Context context, Sector s) {
        Pair<Long, Long> coords = new Pair<Long, Long>(s.getX(), s.getY());
        return mSectors.getBackgroundRenderer(context, coords);
    }

    public Collection<Sector> getSectors() {
        return mSectors.getAllSectors();
    }

    /**
     * Finds the star with the given key.
     */
    public Star findStar(String starKey) {
        return mSectorStars.get(starKey);
    }

    public void addSectorListChangedListener(OnSectorListChangedListener onSectorListChanged) {
        if (mSectorListChangedListeners.contains(onSectorListChanged))
            return;
        mSectorListChangedListeners.add(onSectorListChanged);
    }

    public void removeSectorListChangedListener(OnSectorListChangedListener onSectorListChanged) {
        if (!mSectorListChangedListeners.contains(onSectorListChanged))
            return;
        mSectorListChangedListeners.remove(onSectorListChanged);
    }

    protected void fireSectorListChanged() {
        for(OnSectorListChangedListener listener : mSectorListChangedListeners) {
            listener.onSectorListChanged();
        }
    }

    /**
     * This is called by the StarManager when a star is updated. We care about certain changes to
     * stars (e.g. when they're renamed), but not all...
     */
    public void onStarUpdate(Star star) {
        Star ourStar = findStar(star.getKey());
        if (ourStar != null) {
            if (!ourStar.getName().equals(star.getName())) {
                ourStar.setName(star.getName());
                fireSectorListChanged();
            }
        }
    }

    /**
     * Forces us to refresh the given sector, even if we already have it loaded. Useful when
     * we know it's been modified (by our own actions, for example).
     */
    public void refreshSector(long sectorX, long sectorY) {
        ArrayList<Pair<Long, Long>> coords = new ArrayList<Pair<Long, Long>>();
        coords.add(new Pair<Long, Long>(sectorX, sectorY));
        requestSectors(coords, true, null);
    }

    /**
     * Returns a {@link Point2D} that represents a line segment from {@link Star} a to
     * {@link Star} b. You can use the \c length() method to determine the distance between them.
     */
    public Vector2 directionBetween(StarSummary a, StarSummary b) {
        float dx = a.getOffsetX() - b.getOffsetX();
        float dy = a.getOffsetY() - b.getOffsetY();

        float dsx = a.getSectorX() - b.getSectorX();
        dx += (dsx * SECTOR_SIZE);

        float dsy = a.getSectorY() - b.getSectorY();
        dy += (dsy * SECTOR_SIZE);

        return new Vector2(dx, dy);
    }

    /**
     * Calculates the distance (in "parsecs") between the two given stars.
     */
    public float distanceInParsecs(StarSummary a, StarSummary b) {
        double distanceInPixels = directionBetween(a, b).length();
        return (float) (distanceInPixels / 10.0);
    }

    /**
     * Fetches the details of a bunch of sectors from the server.
     */
    public void requestSectors(final List<Pair<Long, Long>> coords, boolean force,
                               final OnSectorsFetchedListener callback) {
        if (log.isDebugEnabled()) {
            String msg = "";
            for(Pair<Long, Long> coord : coords) {
                if (msg.length() != 0) {
                    msg += ", ";
                }
                msg += String.format("(%d, %d)", coord.one, coord.two);
            }
            log.debug(String.format("Requesting sectors %s...", msg));
        }

        Map<Pair<Long, Long>, Sector> existingSectors = new TreeMap<Pair<Long, Long>, Sector>();
        final List<Pair<Long, Long>> missingSectors = new ArrayList<Pair<Long, Long>>();
        synchronized(this) {
            for (Pair<Long, Long> coord : coords) {
                Sector s = mSectors.get(coord);
                if (s != null && !force) {
                    existingSectors.put(coord, s);
                } else if (mInTransitListeners.containsKey(coord)) {
                    if (callback != null) {
                        List<OnSectorsFetchedListener> listeners = mInTransitListeners.get(coord);
                        listeners.add(callback);
                    }
                } else {
                    missingSectors.add(coord);
                }
            }

            if (!existingSectors.isEmpty() && callback != null) {
                callback.onSectorsFetched(existingSectors);
            }

            if (!missingSectors.isEmpty()) {
                // record the fact that we've now got these sectors in transit
                for (Pair<Long, Long> coord : missingSectors) {
                    mInTransitListeners.put(coord, new ArrayList<OnSectorsFetchedListener>());
                }

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
                            sectors = Sector.fromProtocolBuffer(pb.getSectorsList());
                        } catch(Exception e) {
                            log.error(ExceptionUtils.getStackTrace(e));
                        }

                        return sectors;
                    }

                    @Override
                    protected void onComplete(List<Sector> sectors) {
                        if (sectors == null) {
                            return; // BAD!
                        }

                        Map<Pair<Long, Long>, Sector> theseSectors = null;
                        synchronized(this) {
                            if (callback != null)
                                theseSectors = new TreeMap<Pair<Long, Long>, Sector>();
                            for(Sector s : sectors) {
                                Pair<Long, Long> key = new Pair<Long, Long>(s.getX(), s.getY());
                                log.debug(String.format("Fetched sector (%d, %d)", s.getX(), s.getY()));

                                mSectors.put(key, s);
                                if (callback != null) {
                                    theseSectors.put(key, s);
                                }

                                for (Star star : s.getStars()) {
                                    mSectorStars.put(star.getKey(), star);
                                }

                                Map<Pair<Long, Long>, Sector> thisSector = null;
                                List<OnSectorsFetchedListener> listeners = mInTransitListeners.get(key);
                                if (listeners != null) {
                                    for (OnSectorsFetchedListener listener : listeners) {
                                        if (listener != null) {
                                            if (thisSector == null) {
                                                thisSector = new TreeMap<Pair<Long, Long>, Sector>();
                                                thisSector.put(key, s);
                                            }

                                            listener.onSectorsFetched(thisSector);
                                        }
                                    }
                                }
                            }

                            for (Pair<Long, Long> coord : missingSectors) {
                                mInTransitListeners.remove(coord);
                            }
                        }

                        if (callback != null) {
                            callback.onSectorsFetched(theseSectors);
                        }
                        fireSectorListChanged();
                    }
                }.execute();
            }
        }
    }

    private static class SectorCache {
        BackgroundRendererCache mBackgroundRenderers;
        LruCache<String, Sector> mSectors;

        public SectorCache() {
            mSectors = new LruCache<String, Sector>(30);
            mBackgroundRenderers = new BackgroundRendererCache();
        }

        public static String key(Pair<Long, Long> coord) {
            return String.format(Locale.ENGLISH, "%d:%d", coord.one, coord.two);
        }

        public StarfieldBackgroundRenderer getBackgroundRenderer(Context context, Pair<Long, Long> coords) {
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
                    renderer = new StarfieldBackgroundRenderer(context, seeds);
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
    }

    public interface OnSectorListChangedListener {
        void onSectorListChanged();
    }

    public interface OnSectorsFetchedListener {
        void onSectorsFetched(Map<Pair<Long, Long>, Sector> sectors);
    }
}
