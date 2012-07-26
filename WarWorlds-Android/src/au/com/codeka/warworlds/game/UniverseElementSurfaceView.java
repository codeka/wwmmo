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
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Base class for things like \c StarfieldSurfaceView and \c SolarSystemSurfaceView, etc.
 * @author dean@codeka.com.au
 *
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
            // if we have overlays then we're animated so we need to redraw anyway
            if (mOverlays.size() == 0) {
                try {
                    mDrawSemaphore.acquire();
                } catch (InterruptedException e) {
                    log.info("Surface was destroyed, render thread shutting down.");
                    return;
                }
            }

            SurfaceHolder h = mHolder;
            if (h == null) {
                log.info("Surface was destroyed, render thread shutting down.");
                return;
            }

            Canvas c = h.lockCanvas();
            if (c == null) {
                log.info("Surface was destroyed, render thread shutting down.");
                return;
            }

            try {
                try {
                    onDraw(c);

                    synchronized(mOverlays) {
                        int size = mOverlays.size();
                        for (int i = 0; i < size; i++) {
                            mOverlays.get(i).draw(c);
                        }
                    }
                } finally {
                    h.unlockCanvasAndPost(c);
                }
            } catch(Exception e) {
                log.error("An error occured re-drawing the canvas, ignoring.", e);
            }
        }
    }

    /**
     * An \c Overlay is a little graphic that sits over the top of the surface view. It's drawn
     * separately and can be animated as well. Useful for things like selection indicators and
     * so on.
     */
    protected static abstract class Overlay {
        private boolean mIsVisible;

        private void setVisible(boolean isVisible) {
            mIsVisible = isVisible;
        }

        public boolean isVisible() {
            return mIsVisible;
        }

        public abstract void draw(Canvas canvas);
    }

    /**
     * This overlay is used for drawing the selection indicator. It's an animated dotted circle
     * that spins around the selected point.
     */
    protected static class SelectionOverlay extends Overlay {
        private Paint mPaint;
        private float mCentreX;
        private float mCentreY;
        private float mRadius;
        private float mAngle;
        private float mDashAngleDegrees;
        private float mRotateSpeed;
        private RectF mInnerCircle;
        private RectF mOuterCircle;
        private RectF mRect;

        public SelectionOverlay(double centreX, double centreY, double radius) {
            mCentreX = 0;
            mCentreY = 0;
            mRadius = 0;
            mAngle = 0;

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setARGB(255, 255, 255, 255);
            mPaint.setStyle(Paint.Style.STROKE);

            mInnerCircle = new RectF();
            mOuterCircle = new RectF();
            mRect = new RectF();
            mRotateSpeed = 1.5f;

            setCentre(centreX, centreY);
            setRadius(radius);
        }

        public void setCentre(double x, double y) {
            mCentreX = (float) x;
            mCentreY = (float) y;
            setRadius(mRadius);
        }

        public void setRadius(double radius) {
            mRadius = (float) radius;

            int numDashes = 40; // we want 40 dashes around the circle
            double dashAngleRadians = (2 * Math.PI) / numDashes;
            double dashPixels = dashAngleRadians * radius;
            mDashAngleDegrees = 360.0f / numDashes;

            DashPathEffect pathEffect = new DashPathEffect(new float[] {
                    (float) dashPixels, (float) dashPixels}, 1);
            mPaint.setPathEffect(pathEffect);

            mInnerCircle.left = mCentreX - mRadius;
            mInnerCircle.top = mCentreY - mRadius;
            mInnerCircle.right = mCentreX + mRadius;
            mInnerCircle.bottom = mCentreY + mRadius;

            mOuterCircle.left = mCentreX - mRadius - 4;
            mOuterCircle.top = mCentreY - mRadius - 4;
            mOuterCircle.right = mCentreX + mRadius + 4;
            mOuterCircle.bottom = mCentreY + mRadius + 4;
        }

        @Override
        public void draw(Canvas canvas) {
            mAngle += mRotateSpeed;
            if (mAngle > 360.0f) {
                mAngle = 0.0f;
            }

            // we cannot make the arc 360 degrees, otherwise it ignores the angle offset. Instead
            // we make it 360 - "dash-angle" (i.e. the angle through which one dash passes)
            canvas.drawArc(mInnerCircle, mAngle, 360.0f - mDashAngleDegrees, false, mPaint);
            canvas.drawArc(mOuterCircle, 360.0f - mAngle, 360.0f - mDashAngleDegrees, false, mPaint);
        }
    }
}
