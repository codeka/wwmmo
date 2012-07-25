package au.com.codeka.warworlds.game;

import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
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

    private Thread mDrawThread;
    private Semaphore mDrawSemaphore;

    public UniverseElementSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mPixelScale = context.getResources().getDisplayMetrics().density;

        if (this.isInEditMode()) {
            return;
        }

        mDrawSemaphore = new Semaphore(1);
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
                mDrawSemaphore.acquire();
            } catch (InterruptedException e) {
                log.info("Surface was destroyed, render thread shutting down.");
                return;
            }

            SurfaceHolder h = mHolder;
            Canvas c = h.lockCanvas();
            try {
                try {
                    onDraw(c);
                } finally {
                    h.unlockCanvasAndPost(c);
                }
            } catch(Exception e) {
                log.error("An error occured re-drawing the canvas, ignoring.", e);
            }
        }
    }
}
