package au.com.codeka.warworlds.game;

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.Star;

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSurfaceView extends UniverseElementSurfaceView {
    private static String TAG = "StarfieldSurfaceView";

    private GestureDetector mGestureDetector;
    private GestureHandler mGestureHandler;
    private CopyOnWriteArrayList<OnStarSelectedListener> mStarSelectedListeners;
    private Star mSelectedStar;

    public StarfieldSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        Log.i(TAG, "Starfield initializing...");

        mStarSelectedListeners = new CopyOnWriteArrayList<OnStarSelectedListener>();
        mSelectedStar = null;

        mGestureHandler = new GestureHandler();
        mGestureDetector = new GestureDetector(context, mGestureHandler);

        SectorManager.getInstance().addSectorListChangedListener(new SectorManager.OnSectorListChangedListener() {
            @Override
            public void onSectorListChanged() {
                redraw();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
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

        // clear it to black
        canvas.drawColor(0xff000000);

        for(int y = -sm.getRadius(); y <= sm.getRadius(); y++) {
            for(int x = -sm.getRadius(); x <= sm.getRadius(); x++) {
                long sectorX = sm.getSectorCentreX() + x;
                long sectorY = sm.getSectorCentreY() + y;

                Sector sector = sm.getSector(sectorX, sectorY);
                if (sector == null) {
                    continue; // it might not be loaded yet...
                }

                drawSector(canvas, (x * SectorManager.SECTOR_SIZE) + sm.getOffsetX(),
                        (y * SectorManager.SECTOR_SIZE) + sm.getOffsetY(), sector);
            }
        }
    }

    private void drawSector(Canvas canvas, int offsetX, int offsetY, Sector sector) {
        for(Star star : sector.getStars()) {
            drawStar(canvas, star, offsetX, offsetY);
        }
    }

    private Paint p = null;
    private void drawStar(Canvas canvas, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        int[] colours = { star.getColour(), star.getColour(), 0x00000000 };
        float[] positions = { 0.0f, 0.4f, 1.0f };

        RadialGradient gradient = new RadialGradient(x, y, star.getSize(), 
                colours, positions, android.graphics.Shader.TileMode.CLAMP);
        if (p == null) {
            p = new Paint();
            p.setDither(true);
        }
        p.setShader(gradient);

        canvas.drawCircle(x, y, star.getSize(), p);

        if (mSelectedStar == star) {
            Paint p2 = new Paint();
            p2.setARGB(255, 255, 255, 255);
            p2.setStyle(Style.STROKE);
            canvas.drawCircle(x, y, star.getSize() + 5, p2);
        }
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    private class GestureHandler extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {

            Log.i(TAG, "Dragged, ("+distanceX+", "+distanceY+")");

            SectorManager.getInstance().scroll(-(int)distanceX, -(int)distanceY);
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
            int tapX = (int) e.getX();
            int tapY = (int) e.getY();

            Star star = SectorManager.getInstance().getStarAt(tapX, tapY);
            if (star != null) {
                Log.i(TAG, "Star at ("+star.getOffsetX()+", "+star.getOffsetY()+") tapped ("+tapX+", "+tapY+").");
                mSelectedStar = star;
                redraw();
                fireStarSelected(star);
            } else {
                Log.i(TAG, "No star tapped, tap = ("+tapX+", "+tapY+")");
            }

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
