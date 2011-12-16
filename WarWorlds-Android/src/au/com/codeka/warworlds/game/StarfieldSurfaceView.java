package au.com.codeka.warworlds.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class StarfieldSurfaceView extends SurfaceView
                implements SurfaceHolder.Callback {
    private static String TAG = "StarfieldSurfaceView";

    private SurfaceHolder mHolder;

    public StarfieldSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG, "Starfield initializing...");
        getHolder().addCallback(this);
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

    /**
     * Causes the \c StarfieldSurfaceView to redraw itself. Used, eg, when we
     * scroll, etc.
     */
    public void redraw() {
        final SurfaceHolder h = mHolder;
        if (h == null) {
            return;
        }

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
        }.execute();

    }

    @Override
    public void onDraw(Canvas canvas) {
        Log.i(TAG, "SurfaceView.onDraw()");

        SectorManager sm = SectorManager.getInstance();

        for(long sectorY = sm.getSectorCentreY() - sm.getRadius();
                sectorY <= sm.getSectorCentreY() + sm.getRadius();
                sectorY++) {
            for(long sectorX = sm.getSectorCentreX() - sm.getRadius();
                    sectorX <= sm.getSectorCentreX() + sm.getRadius();
                    sectorX++) {
                Log.i(TAG, "Getting sector at ("+sectorX+","+sectorY+")");

                StarfieldSector sector = sm.getSector(sectorX, sectorY);
                if (sector == null) {
                    Log.w(TAG, "No sector at ("+sectorX+","+sectorY+")...");
                    // TODO: huh??
                    continue;
                }

                drawSector(canvas, 0, 0, sector);
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
                    drawStar(canvas, star, nodeX * 32, nodeY * 32);
                }
            }
        }
    }

    private void drawStar(Canvas canvas, StarfieldStar star, int x, int y) {
        // TODO: it's crazy to create the gradient/paint anew for each star
        RadialGradient gradient = new RadialGradient(x, y, 10, 0xFFFFFFFF,
                0xFF000000, android.graphics.Shader.TileMode.CLAMP);
        Paint p = new Paint();
        p.setDither(true);
        p.setShader(gradient);

        Log.i(TAG, "canvas.drawCircle("+x+","+y+", 10.0f, p)");
        canvas.drawCircle(x, y, 10.0f, p);
    }
}
