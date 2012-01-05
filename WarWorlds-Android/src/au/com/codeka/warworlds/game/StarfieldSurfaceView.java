package au.com.codeka.warworlds.game;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import au.com.codeka.warworlds.shared.StarfieldSector;
import au.com.codeka.warworlds.shared.StarfieldStar;
import au.com.codeka.warworlds.shared.constants.SectorConstants;

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static String TAG = "StarfieldSurfaceView";

    private SurfaceHolder mHolder;
    private GestureDetector mGestureDetector;
    private GestureHandler mGestureHandler;
    private ArrayList<OnStarSelectedListener> mStarSelectedListeners;
    private StarfieldStar mSelectedStar;

    // these are used to ensure we don't queue up heaps of AsyncTasks for
    // redrawing the screen.
    private boolean mIsRedrawing = false;
    private boolean mNeedsRedraw = false;

    public StarfieldSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG, "Starfield initializing...");

        mSelectedStar = null;

        getHolder().addCallback(this);
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated...");
        mHolder = holder;

        redraw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;
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

    protected void fireStarSelected(StarfieldStar star) {
        for(OnStarSelectedListener listener : mStarSelectedListeners) {
            listener.onStarSelected(star);
        }
    }

    /**
     * Causes the \c StarfieldSurfaceView to redraw itself. Used, eg, when we
     * scroll, etc.
     * 
     * If we've already scheduled a redraw when you call this, the redraw is
     * "queued" until the currently-executing redraw finishes.
     */
    public void redraw() {
        final SurfaceHolder h = mHolder;
        if (h == null) {
            return;
        }

        if (mIsRedrawing) {
            mNeedsRedraw = true;
            return;
        }
        mIsRedrawing = true;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                Canvas c = h.lockCanvas();
                try {
                    synchronized(h) {
                        onDraw(c);
                    }
                } finally {
                    h.unlockCanvasAndPost(c);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mIsRedrawing = false;

                // if another redraw was scheduled, do it now
                if (mNeedsRedraw) {
                    mNeedsRedraw = false;
                    redraw();
                }
            }
        }.execute();
    }

    /**
     * Draws the actual starfield to the given \c Canvas. This will be called in
     * a background thread, so we can't do anything UI-specific, except drawing
     * to the canvas.
     */
    @Override
    public void onDraw(Canvas canvas) {
        SectorManager sm = SectorManager.getInstance();

        // clear it to black
        canvas.drawColor(0xff000000);

        for(int y = -sm.getRadius(); y <= sm.getRadius(); y++) {
            for(int x = -sm.getRadius(); x <= sm.getRadius(); x++) {
                long sectorX = sm.getSectorCentreX() + x;
                long sectorY = sm.getSectorCentreY() + y;

                StarfieldSector sector = sm.getSector(sectorX, sectorY);
                if (sector == null) {
                    continue; // it might not be loaded yet...
                }

                drawSector(canvas, (x * SectorConstants.Width) + sm.getOffsetX(),
                        (y * SectorConstants.Height) + sm.getOffsetY(), sector);
            }
        }
    }

    private void drawSector(Canvas canvas, int offsetX, int offsetY,
            StarfieldSector sector) {
        for(StarfieldStar star : sector.getStars()) {
            drawStar(canvas, star, offsetX, offsetY);
        }
    }

    private Paint p = null;
    private void drawStar(Canvas canvas, StarfieldStar star, int x, int y) {
        x += star.getX();
        y += star.getY();

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

            StarfieldStar star = SectorManager.getInstance().getStarAt(tapX, tapY);
            if (star != null) {
                Log.i(TAG, "Star at ("+star.getX()+", "+star.getY()+") tapped ("+tapX+", "+tapY+").");
                mSelectedStar = star;
                redraw();
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
        public abstract void onStarSelected(StarfieldStar star);
    }
}
