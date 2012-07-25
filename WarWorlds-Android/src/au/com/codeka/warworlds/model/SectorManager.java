package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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

    private int mRadius = 1;

    private long mSectorX;
    private long mSectorY;
    private float mOffsetX;
    private float mOffsetY;
    private Map<Pair<Long, Long>, Sector> mSectors;
    private Set<Pair<Long, Long>> mInTransitSectors;
    private CopyOnWriteArrayList<OnSectorListChangedListener> mSectorListChangedListeners;

    public static int SECTOR_SIZE = 1024;

    private SectorManager() {
        mSectorX = mSectorY = 0;
        mOffsetX = mOffsetY = 0;
        mSectors = new TreeMap<Pair<Long, Long>, Sector>();
        mInTransitSectors = new TreeSet<Pair<Long, Long>>();
        mSectorListChangedListeners = new CopyOnWriteArrayList<OnSectorListChangedListener>();
        this.scrollTo(0, 0, 0, 0);
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
     * Gets the x-coordinate of the sector that's in the "centre" of the screen.
     */
    public long getSectorCentreX() {
        return mSectorX;
    }

    /**
     * Gets the y-coordinate of the sector that's in the "centre" of the screen.
     */
    public long getSectorCentreY() {
        return mSectorY;
    }

    /**
     * Gets an offset, in pixels, that we apply to the sectors (to facilitate
     * smooth scrolling of the sectors).
     */
    public int getOffsetX() {
        return (int) mOffsetX;
    }

    /**
     * Gets an offset, in pixels, that we apply to the sectors (to facilitate
     * smooth scrolling of the sectors).
     */
    public int getOffsetY() {
        return (int) mOffsetY;
    }

    /**
     * Gets the "radius" of sectors, around the centre one, that we should
     * use to render a complete "screen".
     */
    public int getRadius() {
        return mRadius;
    }

    /**
     * Scroll to the given sector (x,y) and offset into the sector.
     */
    public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY) {
        mSectorX = sectorX;
        mSectorY = sectorY;
        mOffsetX = -offsetX;
        mOffsetY = -offsetY;

        List<Pair<Long, Long>> missingSectors = new ArrayList<Pair<Long, Long>>();

        Map<Pair<Long, Long>, Sector> newSectors = 
                new TreeMap<Pair<Long, Long>, Sector>();
        for(sectorY = mSectorY - mRadius; sectorY <= mSectorY + mRadius; sectorY++) {
            for(sectorX = mSectorX - mRadius; sectorX <= mSectorX + mRadius; sectorX++) {
                Pair<Long, Long> key = new Pair<Long, Long>(sectorX, sectorY);
                if (mSectors.containsKey(key)) {
                    newSectors.put(key, mSectors.get(key));
                } else {
                    // we need to fetch the sector from the server, check that
                    // we're not already in the process of fetching it and if
                    // not add it to a list to be fetched...
                    if (!mInTransitSectors.contains(key)) {
                        missingSectors.add(key);
                    }
                }
            }
        }

        if (!missingSectors.isEmpty()) {
            requestSectors(missingSectors);
        }

        mSectors = newSectors;
    }

    /**
     * Scrolls the view by a relative amount.
     * @param distanceX Number of pixels in the X direction to scroll.
     * @param distanceY Number of pixels in the Y direction to scroll.
     */
    public void scroll(float distanceX, float distanceY) {
        mOffsetX += distanceX;
        mOffsetY += distanceY;

        boolean needUpdate = false;
        while (mOffsetX < -(SECTOR_SIZE / 2)) {
            mOffsetX += SECTOR_SIZE;
            mSectorX ++;
            needUpdate = true;
        }
        while (mOffsetX > (SECTOR_SIZE / 2)) {
            mOffsetX -= SECTOR_SIZE;
            mSectorX --;
            needUpdate = true;
        }
        while (mOffsetY < -(SECTOR_SIZE / 2)) {
            mOffsetY += SECTOR_SIZE;
            mSectorY ++;
            needUpdate = true;
        }
        while (mOffsetY > (SECTOR_SIZE / 2)) {
            mOffsetY -= SECTOR_SIZE;
            mSectorY --;
            needUpdate = true;
        }

        if (needUpdate) {
            scrollTo(mSectorX, mSectorY, -mOffsetX, -mOffsetY);
        }
    }

    /**
     * Gets the \c Star that's closest to the given (x,y), based on the current sector
     * centre and offsets.
     */
    public Star getStarAt(int viewX, int viewY) {
        // first, work out which sector your actually inside of. If (mOffsetX, mOffsetY) is (0,0)
        // then (x,y) corresponds exactly to the offset into (mSectorX, mSectorY). Otherwise, we
        // have to adjust (x,y) by the offset so that it works out like that.
        int x = viewX - (int) mOffsetX;
        int y = viewY - (int) mOffsetY;

        long sectorX = mSectorX;
        long sectorY = mSectorY;
        while (x < 0) {
            x += SECTOR_SIZE;
            sectorX --;
        }
        while (x >= SECTOR_SIZE) {
            x -= SECTOR_SIZE;
            sectorX ++;
        }
        while (y < 0) {
            y += SECTOR_SIZE;
            sectorY --;
        }
        while (y >= SECTOR_SIZE) {
            y -= SECTOR_SIZE;
            sectorY ++;
        }

        Sector sector = getSector(sectorX, sectorY);
        if (sector == null) {
            // if it's not loaded yet, you can't have tapped on anything...
            return null;
        }

        int minDistance = 0;
        Star closestStar = null;

        for(Star star : sector.getStars()) {
            int starX = star.getOffsetX();
            int starY = star.getOffsetY();

            int distance = (starX - x)*(starX - x) + (starY - y)*(starY - y);
            if (closestStar == null || distance < minDistance) {
                closestStar = star;
                minDistance = distance;
            }
        }

        // only return it if you tapped within a 48 pixel radius
        if (Math.sqrt(minDistance) <= 48) {
            return closestStar;
        }
        return null;
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
        requestSectors(coords);
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
    private void requestSectors(final List<Pair<Long, Long>> coords) {
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

        new AsyncTask<Void, Void, List<Sector>>() {
            @Override
            protected List<Sector> doInBackground(Void... arg0) {
                List<Sector> sectors = null;

                String url = "";
                for(Pair<Long, Long> coord : coords) {
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

                for(Sector s : sectors) {
                    Pair<Long, Long> key = new Pair<Long, Long>(
                            s.getX(), s.getY());
                    mSectors.put(key, s);
                }
                mInTransitSectors.clear();

                // let everybody know the list of sectors has been updated
                fireSectorListChanged();
            }
        }.execute();
    }

    /**
     * This interface should be implemented when you want to listen for "sector list changed"
     * event (which happens when a new sector is loaded).
     */
    public interface OnSectorListChangedListener extends EventListener {
        /**
         * This is called when the sector list changes (i.e. when a new sector(s) is loaded)
         */
        public abstract void onSectorListChanged();
    }
}
