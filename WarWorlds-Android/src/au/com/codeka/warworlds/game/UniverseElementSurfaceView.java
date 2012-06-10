package au.com.codeka.warworlds.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
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
    private boolean mIsRedrawing;
    private boolean mNeedsRedraw;
    private GestureDetector mGestureDetector;
    private boolean mDisableGestures = false;
    GestureDetector.OnGestureListener mGestureListener;
    private float mPixelScale;

    public UniverseElementSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mPixelScale = context.getResources().getDisplayMetrics().density;

        if (this.isInEditMode()) {
            return;
        }

        getHolder().addCallback(this);
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

    protected void redraw() {
        if (isInEditMode()) {
            return;
        }

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
                    try {
                        synchronized(h) {
                            onDraw(c);
                        }
                    } finally {
                        h.unlockCanvasAndPost(c);
                    }
                } catch(Exception e) {
                    log.error("An error occured re-drawing the canvas, ignoring.", e);
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
}
