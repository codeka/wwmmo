package au.com.codeka.warworlds.game;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import au.com.codeka.warworlds.GlobalOptions;

/**
 * Handles drawing a starfield background. Used by the \c StarfieldSurfaceView and
 * \c SolarSystemSurfaceView.
 */
public class StarfieldBackgroundRenderer {
    private Logger log = LoggerFactory.getLogger(StarfieldBackgroundRenderer.class);
    private float mPixelScale;
    private GlobalOptions.StarfieldDetail mStarfieldDetail;
    private long[] mSeeds;
    private Paint mBackgroundPaint;
    private WeakReference<Bitmap> mPreRendered;

    private static List<Bitmap> sBgStars;
    private static List<Bitmap> sBgGases;

    public StarfieldBackgroundRenderer(Context context, long[] seeds) {
        mPixelScale = context.getResources().getDisplayMetrics().density;

        mSeeds = seeds;
        if (mSeeds.length != 9) {
            Random r = new Random(mSeeds[0]);
            mSeeds = new long[9];
            for (int i = 0; i < 9; i++) {
                mSeeds[i] = r.nextLong();
            }
        }

        initialize(context);
    }

    public void drawBackground(Canvas canvas, float left, float top, float right, float bottom) {
        if (mPreRendered != null) {
            Bitmap bmp = mPreRendered.get();
            if (bmp == null) {
                bmp = reload();
            }

            Rect src = new Rect(0, 0, 512, 512);
            RectF dest = new RectF(left * mPixelScale, top * mPixelScale, right * mPixelScale, bottom * mPixelScale);
            canvas.drawBitmap(bmp, src, dest, mBackgroundPaint);
        } else {
            canvas.drawRect(left * mPixelScale,
                            top * mPixelScale,
                            right * mPixelScale,
                            bottom * mPixelScale, mBackgroundPaint);
        }
    }

    public void close() {
        if (mPreRendered != null) {
            Bitmap bmp = mPreRendered.get();
            if (bmp != null) {
                bmp.recycle();
            }
        }
    }

    private void initialize(Context context) {
        GlobalOptions globalOptions = new GlobalOptions(context);
        mStarfieldDetail = globalOptions.getStarfieldDetail();

        AssetManager assetMgr = context.getAssets();
        if (sBgStars == null && shouldDrawStars()) {
            sBgStars = loadBitmaps(assetMgr, "decoration/starfield");
        }
        if (sBgGases == null && shouldDrawGas()) {
            sBgGases = loadBitmaps(assetMgr, "decoration/gas");
        }

        if (shouldDrawStars() || shouldDrawGas()) {
            reload();
        } else {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setStyle(Style.FILL);
            mBackgroundPaint.setARGB(255, 0, 0, 0);
        }
    }

    private Bitmap reload() {
        Bitmap bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        render(bmp);
        mPreRendered = new WeakReference<Bitmap>(bmp);
        return bmp;
    }

    /**
     * Renders the background to the given bitmap, which we can then use to
     * render the background again later.
     */
    private void render(Bitmap bmp) {
        Canvas c = new Canvas(bmp);

        // start off black
        c.drawColor(Color.BLACK);

        Rect src;
        RectF dest;
        Random r = new Random(mSeeds[4]);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStyle(Style.STROKE);
        mBackgroundPaint.setARGB(255, 255, 255, 255);

        if (shouldDrawStars()) {
            src = new Rect(0, 0, 512, 512);
            dest = new RectF(0, 0, 512, 512);
            c.drawBitmap(sBgStars.get(r.nextInt(sBgStars.size())), src, dest, mBackgroundPaint);
        }

        if (shouldDrawGas()) {
            for (int iy = -1; iy <= 1; iy++) {
                for (int ix = -1; ix <= 1; ix++) {
                    long seed = mSeeds[((iy + 1) * 3) + (ix + 1)];
                    Random xyr = new Random(seed);
                    for (int i = 0; i < 10; i++) {
                        Bitmap gas = sBgGases.get(xyr.nextInt(sBgGases.size()));

                        src = new Rect(0, 0, gas.getWidth(), gas.getHeight());
                        float x = xyr.nextInt(bmp.getWidth() + 128) - 64;
                        float y = xyr.nextInt(bmp.getWidth() + 128) - 64;
                        dest = new RectF(
                                x - (src.width() / 2) + (ix * bmp.getWidth()),
                                y - (src.height() / 2) + (iy * bmp.getHeight()),
                                x + (src.width() / 2) + (ix * bmp.getWidth()),
                                y + (src.height() / 2) + (iy * bmp.getHeight()));

                        c.drawBitmap(gas, src, dest, mBackgroundPaint);
                    }
                }
            }
        }
    }

    private boolean shouldDrawStars() {
        return mStarfieldDetail.getValue() >= GlobalOptions.StarfieldDetail.STARS.getValue();
    }
    private boolean shouldDrawGas() {
        return mStarfieldDetail.getValue() >= GlobalOptions.StarfieldDetail.STARS_AND_GAS.getValue();
    }

    /**
     * Loads all bitmaps from a given asset subfolder into an array.
     */
    private List<Bitmap> loadBitmaps(AssetManager assetMgr, String basePath) {
        ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();

        String[] fileNames = null;
        try {
            fileNames = assetMgr.list(basePath);
        } catch(IOException e) {
            return bitmaps; // should never happen!
        }

        for(String fileName : fileNames) {
            String fullPath = basePath+"/"+fileName;

            InputStream ins = null;
            try {
                log.info("loading "+fullPath+"...");
                ins = assetMgr.open(fullPath);
                bitmaps.add(BitmapFactory.decodeStream(ins));
            } catch (IOException e) {
                log.error("Error loading image "+fullPath, e); //??
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return bitmaps;
    }
}
