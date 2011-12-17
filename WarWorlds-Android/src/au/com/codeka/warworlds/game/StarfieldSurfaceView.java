package au.com.codeka.warworlds.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class StarfieldSurfaceView extends SurfaceView
                implements SurfaceHolder.Callback {
    private static String TAG = "StarfieldSurfaceView";

    private SurfaceHolder mHolder;
    private GestureDetector mGestureDetector;
    private GestureHandler mGestureHandler;

    // these are used to ensure we don't queue up heaps of AsyncTasks for
    // redrawing the screen.
    private boolean mIsRedrawing = false;
    private boolean mNeedsRedraw = false;

    public StarfieldSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG, "Starfield initializing...");

        getHolder().addCallback(this);
        mGestureHandler = new GestureHandler();
        mGestureDetector = new GestureDetector(context, mGestureHandler);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
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
                    // TODO: huh??
                    continue;
                }

                drawSector(canvas, (x * 512) + sm.getOffsetX(), (y * 512) + sm.getOffsetY(), sector);
            }
        }
    }

    private void drawSector(Canvas canvas, int offsetX, int offsetY,
            StarfieldSector sector) {
        for(int nodeY = 0; nodeY < 16; nodeY++) {
            for(int nodeX = 0; nodeX < 16; nodeX++) {
                StarfieldNode node = sector.getNode(nodeX, nodeY);
                StarfieldStar star = node.getStar();
                if (star != null) {
                    drawStar(canvas, star, offsetX + (nodeX * 32), offsetY + (nodeY * 32));
                }
            }
        }
    }

    private Paint p = null;
    private void drawStar(Canvas canvas, StarfieldStar star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        int[] colours = { star.getColour(), star.getColour(), 0x00000000 };
        float[] positions = { 0.0f, 0.2f, 1.0f };

        RadialGradient gradient = new RadialGradient(x, y, star.getSize(), 
                colours, positions, android.graphics.Shader.TileMode.CLAMP);
        if (p == null) {
            p = new Paint();
            p.setDither(true);
        }
        p.setShader(gradient);

        canvas.drawCircle(x, y, star.getSize(), p);
    }

    /**
     * Implements the \c OnGestureListener methods that we use to respond to
     * various touch events.
     */
    private class GestureHandler extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            SectorManager.getInstance().scroll(-(int)distanceX, -(int)distanceY);
            redraw(); // todo: something better? e.g. event listener or something
            return false;
        }
    }
}
