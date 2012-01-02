package au.com.codeka.warworlds.game;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.exception.ExceptionUtils;

import android.os.AsyncTask;
import android.util.Log;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.shared.StarfieldSector;
import au.com.codeka.warworlds.shared.StarfieldSectorResource;
import au.com.codeka.warworlds.shared.constants.SectorConstants;
import au.com.codeka.warworlds.shared.util.Pair;

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
    private static String TAG = "SectorManager";
    private static SectorManager sInstance = new SectorManager();

    public static SectorManager getInstance() {
        return sInstance;
    }

    private int mRadius = 1;

    private long mSectorX;
    private long mSectorY;
    private int mOffsetX;
    private int mOffsetY;
    private Map<Pair<Long, Long>, StarfieldSector> mSectors;
    private Set<Pair<Long, Long>> mInTransitSectors;

    private SectorManager() {
        mSectorX = mSectorY = 0;
        mOffsetX = mOffsetY = 0;
        mSectors = new TreeMap<Pair<Long, Long>, StarfieldSector>();
        mInTransitSectors = new TreeSet<Pair<Long, Long>>();
        this.scrollTo(0, 0, 0, 0);
    }

    public StarfieldSector getSector(long sectorX, long sectorY) {
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
        return mOffsetX;
    }

    /**
     * Gets an offset, in pixels, that we apply to the sectors (to facilitate
     * smooth scrolling of the sectors).
     */
    public int getOffsetY() {
        return mOffsetY;
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
    public void scrollTo(long sectorX, long sectorY, int offsetX, int offsetY) {
        mSectorX = sectorX;
        mSectorY = sectorY;
        mOffsetX = offsetX;
        mOffsetY = offsetY;

        Set<Pair<Long, Long>> missingSectors = new TreeSet<Pair<Long, Long>>();

        Map<Pair<Long, Long>, StarfieldSector> newSectors = 
                new TreeMap<Pair<Long, Long>, StarfieldSector>();
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
     * Fetches a new sector's details from the server. This will be called when
     * a sector scrolls into view. For a while (until the server responds), the
     * sector will be "null" so you need to take that into account when calling
     * \c getSector().
     * 
     * @param sectorX The x-coordinate of the sector to fetch.
     * @param sectorY The y-coordinate of the sector to fetch.
     */
    private void requestSectors(final Iterable<Pair<Long, Long>> coords) {
        new AsyncTask<Void, Void, List<StarfieldSector>>() {
            @Override
            protected List<StarfieldSector> doInBackground(Void... arg0) {
                List<StarfieldSector> sectors = null;

                try {
                    String url = "/sectors?coords=";
                    for(Pair<Long, Long> pair : coords) {
                        url += pair.one+":"+pair.two+",";
                    }
                    // strip off the last ","
                    url = url.substring(0, url.length() - 1);

                    StarfieldSectorResource resource = Util.getClientResource(
                            url, StarfieldSectorResource.class);
                    sectors = resource.getSectors();
                    if (sectors == null) {
                        // TODO: it's most likely that we need to re-authenticate...
                    }
                } catch(Exception e) {
                    // TODO: handle exceptions
                    Log.e(TAG, ExceptionUtils.getStackTrace(e));
                    // message = "<pre>"+ExceptionUtils.getStackTrace(e)+"</pre>";
                }
                return sectors;
            }

            @Override
            protected void onPostExecute(List<StarfieldSector> sectors) {
                if (sectors == null) {
                    return; // BAD!
                }

                for(StarfieldSector s : sectors) {
                    Pair<Long, Long> key = new Pair<Long, Long>(
                            s.getSectorX(), s.getSectorY());
                    mSectors.put(key, s);
                }
                mInTransitSectors.clear();

                // TODO: view.redraw()
            }
        }.execute();
    }

    /**
     * Scrolls the view by a relative amount.
     * @param distanceX Number of pixels in the X direction to scroll.
     * @param distanceY Number of pixels in the Y direction to scroll.
     */
    public void scroll(int distanceX, int distanceY) {
        mOffsetX += distanceX;
        mOffsetY += distanceY;

        boolean needUpdate = false;
        while (mOffsetX < -(SectorConstants.Width / 2)) {
            mOffsetX += SectorConstants.Width;
            mSectorX ++;
            needUpdate = true;
        }
        while (mOffsetX > (SectorConstants.Width / 2)) {
            mOffsetX -= SectorConstants.Width;
            mSectorX --;
            needUpdate = true;
        }
        while (mOffsetY < -(SectorConstants.Height / 2)) {
            mOffsetY += SectorConstants.Height;
            mSectorY ++;
            needUpdate = true;
        }
        while (mOffsetY > (SectorConstants.Height / 2)) {
            mOffsetY -= SectorConstants.Height;
            mSectorY --;
            needUpdate = true;
        }

        if (needUpdate) {
            scrollTo(mSectorX, mSectorY, mOffsetX, mOffsetY);
        }
    }
}
