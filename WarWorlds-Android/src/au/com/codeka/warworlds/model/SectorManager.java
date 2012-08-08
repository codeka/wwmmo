package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import au.com.codeka.Pair;
import au.com.codeka.warworlds.api.ApiClient;

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
    private static SectorManager sInstance = new SectorManager();

    public static SectorManager getInstance() {
        return sInstance;
    }

    private Map<Pair<Long, Long>, Sector> mSectors;
    private Map<Pair<Long, Long>, List<OnSectorsFetchedListener>> mInTransitListeners;
    private CopyOnWriteArrayList<OnSectorListChangedListener> mSectorListChangedListeners;

    public static int SECTOR_SIZE = 1024;

    private SectorManager() {
        mSectors = new TreeMap<Pair<Long, Long>, Sector>();
        mInTransitListeners = new TreeMap<Pair<Long, Long>, List<OnSectorsFetchedListener>>();
        mSectorListChangedListeners = new CopyOnWriteArrayList<OnSectorListChangedListener>();
    }

    public Sector getSector(long sectorX, long sectorY) {
        Pair<Long, Long> key = new Pair<Long, Long>(sectorX, sectorY);
        if (mSectors.containsKey(key)) {
            return mSectors.get(key);
        } else {
            // it might not be loaded yet...
            return null;
        }
    }

    /**
     * Finds the star with the given key.
     */
    public Star findStar(String starKey) {
        for (Sector sector : mSectors.values()) {
            for (Star star : sector.getStars()) {
                if (star.getKey().equals(starKey)) {
                    return star;
                }
            }
        }

        return null;
    }

    public void addSectorListChangedListener(OnSectorListChangedListener onSectorListChanged) {
        if (mSectorListChangedListeners.contains(onSectorListChanged))
            return;
        mSectorListChangedListeners.add(onSectorListChanged);
    }

    protected void fireSectorListChanged() {
        for(OnSectorListChangedListener listener : mSectorListChangedListeners) {
            listener.onSectorListChanged();
        }
    }

    /**
     * Forces us to refresh the given sector, even if we already have it loaded. Useful when
     * we know it's been modified (by our own actions, for example).
     */
    public void refreshSector(long sectorX, long sectorY) {
        ArrayList<Pair<Long, Long>> coords = new ArrayList<Pair<Long, Long>>();
        coords.add(new Pair<Long, Long>(sectorX, sectorY));
        requestSectors(coords, null);
    }

    /**
     * Fetches the details of a bunch of sectors from the server. We request all sectors
     * in a square.
     * 
     * TODO: that's not very efficient... often we'll only need an L-shape of sectors, no need
     * to request the sector in the middle as well.
     * 
     * @param sectorX1 The minimum X-coordinate of the sector to request.
     * @param sectorY1 The minimum Y-coordinate of the sector to request.
     * @param sectorX2 The maximum X-coordinate of the sector to request.
     * @param sectorY2 The maximum Y-coordinate of the sector to request.
     */
    public void requestSectors(final List<Pair<Long, Long>> coords,
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
        for (Pair<Long, Long> coord : coords) {
            if (mSectors.containsKey(coord)) {
                existingSectors.put(coord, mSectors.get(coord));
            } else if (mInTransitListeners.containsKey(coord) && callback != null) {
                List<OnSectorsFetchedListener> listeners = mInTransitListeners.get(coord);
                listeners.add(callback);
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

            new AsyncTask<Void, Void, List<Sector>>() {
                @Override
                protected List<Sector> doInBackground(Void... arg0) {
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
                        warworlds.Warworlds.Sectors pb = ApiClient.getProtoBuf(url,
                                warworlds.Warworlds.Sectors.class);
                        sectors = Sector.fromProtocolBuffer(pb.getSectorsList());
                    } catch(Exception e) {
                        // TODO: handle exceptions
                        log.error(ExceptionUtils.getStackTrace(e));
                    }

                    return sectors;
                }

                @Override
                protected void onPostExecute(List<Sector> sectors) {
                    if (sectors == null) {
                        return; // BAD!
                    }

                    Map<Pair<Long, Long>, Sector> theseSectors = null;
                    if (callback != null)
                        theseSectors = new TreeMap<Pair<Long, Long>, Sector>();
                    for(Sector s : sectors) {
                        Pair<Long, Long> key = new Pair<Long, Long>(s.getX(), s.getY());
                        log.debug(String.format("Fetched sector (%d, %d)", s.getX(), s.getY()));

                        mSectors.put(key, s);
                        if (callback != null) {
                            theseSectors.put(key, s);
                        }

                        Map<Pair<Long, Long>, Sector> thisSector = null;
                        for (OnSectorsFetchedListener listener : mInTransitListeners.get(key)) {
                            if (listener != null) {
                                if (thisSector == null) {
                                    thisSector = new TreeMap<Pair<Long, Long>, Sector>();
                                    thisSector.put(key, s);
                                }

                                listener.onSectorsFetched(thisSector);
                            }
                        }
                        mInTransitListeners.remove(key);
                    }

                    if (callback != null) {
                        callback.onSectorsFetched(theseSectors);
                    }
                    fireSectorListChanged();
                }
            }.execute();
        }
    }

    public interface OnSectorListChangedListener {
        void onSectorListChanged();
    }

    public interface OnSectorsFetchedListener {
        void onSectorsFetched(Map<Pair<Long, Long>, Sector> sectors);
    }
}
