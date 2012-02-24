package au.com.codeka.warworlds.game;

import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Base class for things like \c StarfieldSurfaceView and \c SolarSystemSurfaceView, etc.
 * @author dean@codeka.com.au
 *
 */
public class UniverseElementSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private boolean mIsRedrawing;
    private boolean mNeedsRedraw;

    public UniverseElementSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
}
