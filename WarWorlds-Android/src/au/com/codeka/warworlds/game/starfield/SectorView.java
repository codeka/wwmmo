package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.common.Pair;
import au.com.codeka.warworlds.game.UniverseElementSurfaceView;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;

/**
 * This is the base class for StarfieldSurfaceView and TacticalMapView, it contains the common code
 * for scrolling through sectors of stars, etc.
 */
public class SectorView extends UniverseElementSurfaceView 
                        implements SectorManager.OnSectorListChangedListener {
    protected boolean mScrollToCentre = false;

    protected int mSectorRadius = 1;
    protected long mSectorX;
    protected long mSectorY;
    protected float mOffsetX;
    protected float mOffsetY;

    public SectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        mSectorX = mSectorY = 0;
        mOffsetX = mOffsetY = 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SectorManager.getInstance().addSectorListChangedListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SectorManager.getInstance().removeSectorListChangedListener(this);
    }

    @Override
    public void onSectorListChanged() {
        redraw();
    }

    /**
     * Scroll to the given sector (x,y) and offset into the sector.
     */
    public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY) {
        scrollTo(sectorX, sectorY, offsetX, offsetY, false);
    }

    /**
     * Scroll to the given sector (x,y) and offset into the sector.
     */
    public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY, boolean centre) {
        mSectorX = sectorX;
        mSectorY = sectorY;
        mOffsetX = -offsetX;
        mOffsetY = -offsetY;

        if (centre) {
            mScrollToCentre = true;
        }

        List<Pair<Long, Long>> missingSectors = new ArrayList<Pair<Long, Long>>();

        for(sectorY = mSectorY - mSectorRadius; sectorY <= mSectorY + mSectorRadius; sectorY++) {
            for(sectorX = mSectorX - mSectorRadius; sectorX <= mSectorX + mSectorRadius; sectorX++) {
                Pair<Long, Long> key = new Pair<Long, Long>(sectorX, sectorY);
                Sector s = SectorManager.getInstance().getSector(sectorX, sectorY);
                if (s == null) {
                    missingSectors.add(key);
                }
            }
        }

        if (!missingSectors.isEmpty()) {
            SectorManager.getInstance().requestSectors(missingSectors, false, null);
        }

        redraw();
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
        while (mOffsetX < -(SectorManager.SECTOR_SIZE / 2)) {
            mOffsetX += SectorManager.SECTOR_SIZE;
            mSectorX ++;
            needUpdate = true;
        }
        while (mOffsetX > (SectorManager.SECTOR_SIZE / 2)) {
            mOffsetX -= SectorManager.SECTOR_SIZE;
            mSectorX --;
            needUpdate = true;
        }
        while (mOffsetY < -(SectorManager.SECTOR_SIZE / 2)) {
            mOffsetY += SectorManager.SECTOR_SIZE;
            mSectorY ++;
            needUpdate = true;
        }
        while (mOffsetY > (SectorManager.SECTOR_SIZE / 2)) {
            mOffsetY -= SectorManager.SECTOR_SIZE;
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
            x += SectorManager.SECTOR_SIZE;
            sectorX --;
        }
        while (x >= SectorManager.SECTOR_SIZE) {
            x -= SectorManager.SECTOR_SIZE;
            sectorX ++;
        }
        while (y < 0) {
            y += SectorManager.SECTOR_SIZE;
            sectorY --;
        }
        while (y >= SectorManager.SECTOR_SIZE) {
            y -= SectorManager.SECTOR_SIZE;
            sectorY ++;
        }

        Sector sector = SectorManager.getInstance().getSector(sectorX, sectorY);
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

    @Override
    public void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }
        super.onDraw(canvas);

        if (mScrollToCentre) {
            scroll(getWidth() / 2.0f / getPixelScale(),
                   getHeight() / 2.0f / getPixelScale());
            mScrollToCentre = false;
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    protected class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            scroll(-(float)(distanceX / getPixelScale()),
                   -(float)(distanceY / getPixelScale()));

            redraw();
            return false;
        }
    }
}
