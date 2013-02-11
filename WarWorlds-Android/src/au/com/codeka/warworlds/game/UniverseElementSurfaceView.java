package au.com.codeka.warworlds.game;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import au.com.codeka.Point2D;

/**
 * Base class for things like \c StarfieldSurfaceView and \c SolarSystemSurfaceView, etc.
 */
public class UniverseElementSurfaceView extends View {
    private Context mContext;
    private GestureDetector mGestureDetector;
    private boolean mDisableGestures = false;
    GestureDetector.OnGestureListener mGestureListener;
    private float mPixelScale;
    private ArrayList<Overlay> mOverlays;

    public UniverseElementSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mPixelScale = context.getResources().getDisplayMetrics().density;

        if (this.isInEditMode()) {
            return;
        }

        mOverlays = new ArrayList<Overlay>();
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

    public float getPixelScale() {
        return mPixelScale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

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

        invalidate();
    }

    protected void drawOverlays(Canvas canvas) {
        synchronized(mOverlays) {
            int size = mOverlays.size();
            for (int i = 0; i < size; i++) {
                mOverlays.get(i).draw(canvas);
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
}
