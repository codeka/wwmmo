package au.com.codeka.warworlds.game.starfield;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;
import au.com.codeka.warworlds.game.UniverseElementSurfaceView;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSurfaceView extends UniverseElementSurfaceView {
    private Logger log = LoggerFactory.getLogger(StarfieldSurfaceView.class);
    private Context mContext;
    private CopyOnWriteArrayList<OnStarSelectedListener> mStarSelectedListeners;
    private Star mSelectedStar;
    private Paint mStarPaint;
    private Paint mStarNamePaint;
    private Paint mSelectionPaint;
    private Bitmap mColonyIcon;
    private StarfieldBackgroundRenderer mBackgroundRenderer;
    private Map<String, Bitmap> mStarBitmaps;

    public StarfieldSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        log.info("Starfield initializing...");

        mContext = context;
        mStarSelectedListeners = new CopyOnWriteArrayList<OnStarSelectedListener>();
        mSelectedStar = null;
        mStarBitmaps = new HashMap<String, Bitmap>();
        mColonyIcon = BitmapFactory.decodeResource(getResources(), R.drawable.starfield_colony);

        mSelectionPaint = new Paint();
        mSelectionPaint.setARGB(255, 255, 255, 255);
        mSelectionPaint.setStyle(Style.STROKE);

        mStarPaint = new Paint();
        mStarPaint.setARGB(255, 255, 255, 255);
        mStarPaint.setStyle(Style.STROKE);

        mBackgroundRenderer = new StarfieldBackgroundRenderer(mContext);

        // whenever the sector list changes (i.e. when we've refreshed from the server),
        // redraw the screen.
        SectorManager.getInstance().addSectorListChangedListener(new SectorManager.OnSectorListChangedListener() {
            @Override
            public void onSectorListChanged() {
                // make sure we re-select the star we had selected before (if any)
                if (mSelectedStar != null) {
                    Star newSelectedStar = SectorManager.getInstance().findStar(mSelectedStar.getKey());
                    // if it's the same instance, that's fine
                    if (newSelectedStar != mSelectedStar) {
                        selectStar(newSelectedStar);
                    }
                }

                // we'll remove our cached star bitmaps as well, they'll be cached in the
                // ImageManager, but this makes sure we don't just use up all the memory...
                mStarBitmaps.clear();

                redraw();
            }
        });

        // whenever a new star bitmap is generated, redraw the screen
        StarImageManager.getInstance().addBitmapGeneratedListener(
                new ImageManager.BitmapGeneratedListener() {
            @Override
            public void onBitmapGenerated(String key, Bitmap bmp) {
                redraw();
            }
        });
    }

    /**
     * Creates the \c OnGestureListener that'll handle our gestures.
     */
    @Override
    protected GestureDetector.OnGestureListener createGestureListener() {
        return new GestureListener();
    }

    public void addStarSelectedListener(OnStarSelectedListener listener) {
        if (!mStarSelectedListeners.contains(listener)) {
            mStarSelectedListeners.add(listener);
        }
    }

    public void removeStarSelectedListener(OnStarSelectedListener listener) {
        mStarSelectedListeners.remove(listener);
    }

    protected void fireStarSelected(Star star) {
        for(OnStarSelectedListener listener : mStarSelectedListeners) {
            listener.onStarSelected(star);
        }
    }

    public Star getSelectedStar() {
        return mSelectedStar;
    }
    public void selectStar(String starKey) {
        Star star = SectorManager.getInstance().findStar(starKey);
        selectStar(star);
    }

    /**
     * Draws the actual starfield to the given \c Canvas. This will be called in
     * a background thread, so we can't do anything UI-specific, except drawing
     * to the canvas.
     */
    @Override
    public void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            // TODO: do something?
            return;
        }

        SectorManager sm = SectorManager.getInstance();
        canvas.drawColor(Color.BLACK);

        for(int y = -sm.getRadius(); y <= sm.getRadius(); y++) {
            for(int x = -sm.getRadius(); x <= sm.getRadius(); x++) {
                long sectorX = sm.getSectorCentreX() + x;
                long sectorY = sm.getSectorCentreY() + y;

                Sector sector = sm.getSector(sectorX, sectorY);
                if (sector == null) {
                    continue; // it might not be loaded yet...
                }

                int sx = (x * SectorManager.SECTOR_SIZE) + sm.getOffsetX();
                int sy = (y * SectorManager.SECTOR_SIZE) + sm.getOffsetY();

                // TODO: seed should be part of the sector (and used for other things too)
                mBackgroundRenderer.drawBackground(canvas, sx, sy,
                        sx+SectorManager.SECTOR_SIZE, sy+SectorManager.SECTOR_SIZE,
                        sectorX ^ sectorY + sectorX);

                drawSector(canvas, sx, sy, sector);
            }
        }
    }

    /**
     * Draws a sector, which is a 1024x1024 area of stars.
     */
    private void drawSector(Canvas canvas, int offsetX, int offsetY, Sector sector) {
        for(Star star : sector.getStars()) {
            drawStar(canvas, star, offsetX, offsetY);
        }
        for(Star star : sector.getStars()) {
            drawStarName(canvas, star, offsetX, offsetY);
        }
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private void drawStar(Canvas canvas, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        final float pixelScale = getPixelScale();

        // only draw the star if it's actually visible...
        if (isVisible(canvas, (x - 100) * pixelScale, (y - 100) * pixelScale,
                              (x + 100) * pixelScale, (y + 100) * pixelScale)) {

            int imageSize = (int)(star.getSize() * star.getStarType().getImageScale() * 2);
            Bitmap starBitmap = mStarBitmaps.get(star.getKey());
            if (starBitmap == null) {
                starBitmap = StarImageManager.getInstance().getBitmap(mContext, star, imageSize);
                mStarBitmaps.put(star.getKey(), starBitmap);
            }
            if (starBitmap != null) {
                canvas.drawBitmap(starBitmap, (x - (imageSize / 2)) * pixelScale,
                        (y - (imageSize / 2)) * pixelScale, mStarPaint);
            }

            if (mSelectedStar == star) {
                canvas.drawCircle(x * pixelScale, y * pixelScale,
                        (star.getSize() + 5) * pixelScale, mSelectionPaint);
            }

            List<Colony> colonies = star.getColonies();
            if (colonies != null && !colonies.isEmpty()) {
                canvas.drawBitmap(mColonyIcon, (x + mColonyIcon.getWidth()) * pixelScale,
                        (y - mColonyIcon.getHeight()) * pixelScale, mSelectionPaint);
            }
        }
    }

    /**
     * Determines whether any part of the given \c Rect will be visible on the given canvas,
     * not looking at the clip rect, just looking at the bounds of the canvas.
     */
    private static boolean isVisible(Canvas canvas, float left, float top, float right, float bottom) {
        if (right < 0 || bottom < 0) {
            return false;
        }
        if (left > canvas.getWidth() || top > canvas.getHeight()) {
            return false;
        }
        return true;
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private void drawStarName(Canvas canvas, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        final float pixelScale = getPixelScale();

        if (mStarNamePaint == null) {
            mStarNamePaint = new Paint();
            mStarNamePaint.setStyle(Style.STROKE);
            mStarNamePaint.setTextSize(15.0f * pixelScale);
        }
        mStarNamePaint.setARGB(255, 255, 255, 255);
        float width = mStarNamePaint.measureText(star.getName()) / pixelScale;
        x -= (width / 2.0f);
        y += star.getSize() + 10.0f;

        canvas.drawText(star.getName(),
                x * pixelScale, y * pixelScale, mStarNamePaint);
    }

    private void selectStar(Star star) {
        if (star != null) {
            log.info("Selecting star: "+star.getKey());
            mSelectedStar = star;
            redraw();
            fireStarSelected(star);
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            SectorManager.getInstance().scroll(
                    -(int)(distanceX / getPixelScale()),
                    -(int)(distanceY / getPixelScale()));
            redraw(); // todo: something better? e.g. event listener or something

            return false;
        }

        /**
         * We need to check whether you tapped on a star, and if so, we'll display some
         * basic info about the star on the status pane. Actually, we'll just fire the
         * "StarSelect" event and let the \c StarfieldActivity do that.
         */
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            int tapX = (int) (e.getX() / getPixelScale());
            int tapY = (int) (e.getY() / getPixelScale());

            Star star = SectorManager.getInstance().getStarAt(tapX, tapY);
            selectStar(star);

            // play the 'click' sound effect
            playSoundEffect(android.view.SoundEffectConstants.CLICK);

            return false;
        }
    }

    /**
     * This interface should be implemented when you want to listen for "star selected"
     * events -- that is, when the user selects a new star (by tapping on it).
     */
    public interface OnStarSelectedListener {
        /**
         * This is called when the user selects (by tapping it) a star. By definition, only
         * one star can be selected at a time.
         */
        public abstract void onStarSelected(Star star);
    }
}
