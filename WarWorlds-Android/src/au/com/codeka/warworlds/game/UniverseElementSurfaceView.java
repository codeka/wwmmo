package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Process;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import au.com.codeka.Point2D;

/**
 * Base class for things like \c StarfieldSurfaceView and \c SolarSystemSurfaceView, etc.
 */
public class UniverseElementSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private Logger log = LoggerFactory.getLogger(UniverseElementSurfaceView.class);
    private Context mContext;
    private SurfaceHolder mHolder;
    private GestureDetector mGestureDetector;
    private boolean mDisableGestures = false;
    GestureDetector.OnGestureListener mGestureListener;
    private float mPixelScale;
    private ArrayList<Overlay> mOverlays;

    private Thread mDrawThread;
    private Semaphore mDrawSemaphore;

    public UniverseElementSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mPixelScale = context.getResources().getDisplayMetrics().density;

        if (this.isInEditMode()) {
            return;
        }

        mOverlays = new ArrayList<Overlay>();
        mDrawSemaphore = new Semaphore(1);
        getHolder().addCallback(this);
    }

    public void addOverlay(Overlay overlay) {
        if (overlay.isVisible())
            return;

        overlay.setPixelScale(mPixelScale);

        synchronized(mOverlays) {
            mOverlays.add(overlay);
            overlay.setVisible(true);
        }
    }

    public void removeOverlay(Overlay overlay) {
        synchronized(mOverlays) {
            mOverlays.remove(overlay);
            overlay.setVisible(false);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mDrawThread != null) {
            log.info(String.format("Surface changed, stopping draw thread %d", mDrawThread.getId()));
            mHolder = null;
            redraw();
            try {
                mDrawThread.join();
            } catch(InterruptedException e) {
            }
            mDrawThread = null;
        }

        mHolder = holder;
        redraw();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
        redraw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;

        // if the draw thread is running, shut it down...
        if (mDrawThread != null) {
            mDrawThread.interrupt();
            mDrawThread = null;
        }
    }

    public float getPixelScale() {
        return mPixelScale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector == null && !mDisableGestures) {
            GestureDetector.OnGestureListener listener = createGestureListener();
            if (listener == null) {
                mDisableGestures = true;
                return true;
            }
            mGestureDetector = new GestureDetector(mContext, listener);
        }

        mGestureDetector.onTouchEvent(event);
        return true;
    }

    /**
     * If you return non-null from this, we'll set up a gesture detector that calls the
     * methods of the object you return whenever the user gestures on the surface.
     */
    protected GestureDetector.OnGestureListener createGestureListener() {
        return null;
    }

    /**
     * Causes this \c UniverseElementSurfaceView to redraw itself.
     */
    public void redraw() {
        if (isInEditMode()) {
            return;
        }

        if (mHolder == null) {
            return;
        }

        if (mDrawThread == null) {
            mDrawThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    drawThreadProc();
                }
            });
            mDrawThread.start();
        }

        try {
            mDrawSemaphore.release();
        } catch (Exception e) {
            log.error("HUH?", e);
        }
    }

    private void drawThreadProc() {
        while (true) {
            try {
                // if we have overlays then we're animated so we need to redraw anyway
                try {
                    mDrawSemaphore.acquire();
                } catch (InterruptedException e) {
                    log.info("Surface was destroyed, render thread shutting down.");
                    mDrawThread = null;
                    return;
                }

                if (!drawOnce()) {
                    mDrawThread = null;
                    return;
                }
            } catch(Exception e) {
                log.error("An error occured re-drawing the canvas, ignoring.", e);
            }
        }
    }

    private boolean drawOnce() {
        SurfaceHolder h = mHolder;
        if (h == null) {
            log.info("Surface was destroyed, render thread shutting down.");
            return false;
        }

        Canvas c = h.lockCanvas();
        if (c == null) {
            log.info("Surface was destroyed, render thread shutting down.");
            return false;
        }

        log.debug("Drawing from thread: "+Process.myTid());
        try {
            onDraw(c);
            log.debug("Drawing complete, rendering overlays: "+Process.myTid());

            synchronized(mOverlays) {
                int size = mOverlays.size();
                for (int i = 0; i < size; i++) {
                    mOverlays.get(i).draw(c);
                }
            }
        } finally {
            log.debug("Unlocking canvas: "+Process.myTid());
            h.unlockCanvasAndPost(c);
        }

        return true;
    }

    /**
     * An \c Overlay is a little graphic that sits over the top of the surface view. It's drawn
     * separately and can be animated as well. Useful for things like selection indicators and
     * so on.
     */
    protected static abstract class Overlay {
        private boolean mIsVisible;
        private float mPixelScale;

        private void setVisible(boolean isVisible) {
            mIsVisible = isVisible;
        }

        public boolean isVisible() {
            return mIsVisible;
        }
        public float getPixelScale() {
            return mPixelScale;
        }
        public void setPixelScale(float pixelScale) {
            mPixelScale = pixelScale;
        }

        public abstract void draw(Canvas canvas);

        /**
         * This helper class draws a rotating circle at the specific (x, y) coordinate with
         * the specified radius & paint. It's useful for selection circles and whatnot.
         */
        public static class DashedCircle {
            private RectF mBounds;
            private double mCentreX;
            private double mCentreY;
            private Paint mPaint;
            private double mRadius;
            private float mDashAngleDegrees;

            public DashedCircle(Paint paint) {
                mCentreX = 0;
                mCentreY = 0;
                mRadius = 0;
                mBounds = new RectF();
                mPaint = paint;
            }

            public void setCentre(double x, double y) {
                mCentreX = x;
                mCentreY = y;
                setRadius(mRadius);
            }

            public void setRadius(double radius) {
                mRadius = radius;

                int numDashes = 40; // we want 40 dashes around the circle
                double dashAngleRadians = (2 * Math.PI) / numDashes;
                double dashPixels = dashAngleRadians * radius;
                mDashAngleDegrees = 360.0f / numDashes;

                DashPathEffect pathEffect = new DashPathEffect(new float[] {
                        (float) dashPixels, (float) dashPixels}, 1);
                mPaint.setPathEffect(pathEffect);

                mBounds.left = (float) (mCentreX - mRadius);
                mBounds.top = (float) (mCentreY - mRadius);
                mBounds.right = (float) (mCentreX + mRadius);
                mBounds.bottom = (float) (mCentreY + mRadius);
            }

            public void draw(Canvas canvas) {
                if (mRadius == 0) {
                    return;
                }

                // we cannot make the arc 360 degrees, otherwise it ignores the angle offset. Instead
                // we make it 360 - "dash-angle" (i.e. the angle through which one dash passes)
                canvas.drawArc(mBounds, 0, 360.0f - mDashAngleDegrees, false, mPaint);
            }
        }
    }

    /**
     * An \c Overlay that's "attached" to a "visible entity" (star or fleet). We'll make sure
     * it's recentred whenever the view scrolls around.
     */
    public static abstract class VisibleEntityAttachedOverlay extends Overlay {
        protected Point2D mCentre;

        public VisibleEntityAttachedOverlay() {
            mCentre = new Point2D();
        }

        public Point2D getCentre() {
            return mCentre;
        }

        public void setCentre(double x, double y) {
            mCentre.x = (float) x;
            mCentre.y = (float) y;
        }
    }

    /**
     * This overlay is used for drawing the selection indicator. It's an animated dotted circle
     * that spins around the selected point.
     */
    protected static class SelectionOverlay extends VisibleEntityAttachedOverlay {
        private DashedCircle mInnerCircle;
        private DashedCircle mOuterCircle;

        public SelectionOverlay() {
            Paint p = new Paint();
            p.setARGB(255, 255, 255, 255);
            p.setAntiAlias(true);
            p.setStyle(Paint.Style.STROKE);

            mInnerCircle = new DashedCircle(p);
            mOuterCircle = new DashedCircle(p);
        }

        @Override
        public void setCentre(double x, double y) {
            super.setCentre(x, y);

            mInnerCircle.setCentre(x, y);
            mOuterCircle.setCentre(x, y);
        }

        public void setRadius(double radius) {
            mInnerCircle.setRadius(radius);
            mOuterCircle.setRadius(radius + 4.0);
        }

        @Override
        public void draw(Canvas canvas) {
            mInnerCircle.draw(canvas);
            mOuterCircle.draw(canvas);
        }
    }
}
