package au.com.codeka.warworlds.game;

import java.util.Map;
import java.util.TreeMap;

import android.os.AsyncTask;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.shared.StarfieldSector;
import au.com.codeka.warworlds.shared.StarfieldSectorResource;
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

    private SectorManager() {
        mSectorX = mSectorY = 0;
        mOffsetX = mOffsetY = 0;
        mSectors = new TreeMap<Pair<Long, Long>, StarfieldSector>();
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

        Map<Pair<Long, Long>, StarfieldSector> newSectors = 
                new TreeMap<Pair<Long, Long>, StarfieldSector>();
        for(sectorY = mSectorY - mRadius; sectorY <= mSectorY + mRadius; sectorY++) {
            for(sectorX = mSectorX - mRadius; sectorX <= mSectorX + mRadius; sectorX++) {
                Pair<Long, Long> key = new Pair<Long, Long>(sectorX, sectorY);
                if (mSectors.containsKey(key)) {
                    newSectors.put(key, mSectors.get(key));
                } else {
                    // we need to fetch the sector from the server
                    requestSector(sectorX, sectorY);
                }
            }
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
    private void requestSector(final long sectorX, final long sectorY) {
        new AsyncTask<Void, Void, StarfieldSector>() {
            @Override
            protected StarfieldSector doInBackground(Void... arg0) {
                StarfieldSector sector = null;

                try {
                    String url = "/starfield/sector/"+sectorX+"/"+sectorY;

                    StarfieldSectorResource resource = Util.getClientResource(
                            url, StarfieldSectorResource.class);
                    sector = resource.getSector();
                    if (sector == null) {
                        // TODO: it's most likely that we need to re-authenticate...
                    }
                } catch(Exception e) {
                    // TODO: handle exceptions
                    // Log.e(TAG, ExceptionUtils.getStackTrace(e));
                    // message = "<pre>"+ExceptionUtils.getStackTrace(e)+"</pre>";
                }
                return sector;
            }

            @Override
            protected void onPostExecute(StarfieldSector sector) {
                Pair<Long, Long> key = new Pair<Long, Long>(sectorX, sectorY);
                mSectors.put(key, sector);

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
        while (mOffsetX < -256) {
            mOffsetX += 512;
            mSectorX ++;
            needUpdate = true;
        }
        while (mOffsetX > 256) {
            mOffsetX -= 512;
            mSectorX --;
            needUpdate = true;
        }
        while (mOffsetY < -256) {
            mOffsetY += 512;
            mSectorY ++;
            needUpdate = true;
        }
        while (mOffsetY > 256) {
            mOffsetY -= 512;
            mSectorY --;
            needUpdate = true;
        }

        if (needUpdate) {
            scrollTo(mSectorX, mSectorY, mOffsetX, mOffsetY);
        }
    }
}
