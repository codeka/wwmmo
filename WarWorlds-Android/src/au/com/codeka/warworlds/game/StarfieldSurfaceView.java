package au.com.codeka.warworlds.game;

import java.util.List;
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
import android.graphics.RadialGradient;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.Star;

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
    private float mPixelScale;

    public StarfieldSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        log.info("Starfield initializing...");

        mContext = context;
        mStarSelectedListeners = new CopyOnWriteArrayList<OnStarSelectedListener>();
        mSelectedStar = null;
        mColonyIcon = BitmapFactory.decodeResource(getResources(), R.drawable.starfield_colony);
        mPixelScale = context.getResources().getDisplayMetrics().density * 0.75f;

        mSelectionPaint = new Paint();
        mSelectionPaint.setARGB(255, 255, 255, 255);
        mSelectionPaint.setStyle(Style.STROKE);

        mBackgroundRenderer = new StarfieldBackgroundRenderer(mContext);

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

        int[] colours = { star.getColour(), star.getColour(), 0x00000000 };
        float[] positions = { 0.0f, 0.4f, 1.0f };

        if (mStarPaint == null) {
            mStarPaint = new Paint();
            mStarPaint.setDither(true);
        }
        RadialGradient gradient = new RadialGradient(x * mPixelScale, y * mPixelScale,
                star.getSize() * mPixelScale, colours, positions,
                android.graphics.Shader.TileMode.CLAMP);
        mStarPaint.setShader(gradient);

        canvas.drawCircle(x * mPixelScale, y * mPixelScale,
                star.getSize() * mPixelScale, mStarPaint);

        if (mSelectedStar == star) {
            canvas.drawCircle(x * mPixelScale, y * mPixelScale,
                    (star.getSize() + 5) * mPixelScale, mSelectionPaint);
        }

        List<Colony> colonies = star.getColonies();
        if (colonies != null && !colonies.isEmpty()) {
            canvas.drawBitmap(mColonyIcon, (x + 10.0f) * mPixelScale,
                    (y - 10.0f) * mPixelScale, mSelectionPaint);
        }
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private void drawStarName(Canvas canvas, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        if (mStarNamePaint == null) {
            mStarNamePaint = new Paint();
            mStarNamePaint.setStyle(Style.STROKE);
        }
        mStarNamePaint.setARGB(255, 255, 255, 255);

        float width = mStarNamePaint.measureText(star.getName());
        x -= (width / 2.0);
        y += star.getSize() + 10;

        canvas.drawText(star.getName(),
                x * mPixelScale, y * mPixelScale, mStarNamePaint);
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
                    -(int)(distanceX / mPixelScale),
                    -(int)(distanceY / mPixelScale));
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
            int tapX = (int) (e.getX() / mPixelScale);
            int tapY = (int) (e.getY() / mPixelScale);

            Star star = SectorManager.getInstance().getStarAt(tapX, tapY);
            selectStar(star);

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
