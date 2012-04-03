package au.com.codeka.warworlds.game;

import java.io.IOException;
import java.io.InputStream;
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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;

/**
 * Handles drawing a starfield background. Used by the \c StarfieldSurfaceView and
 * \c SolarSystemSurfaceView.
 */
public class StarfieldBackgroundRenderer {
    private Logger log = LoggerFactory.getLogger(StarfieldBackgroundRenderer.class);
    private Context mContext;
    private Paint mBackgroundPaint;
    private List<Bitmap> mBgStars;
    private List<Bitmap> mBgGases;

    public StarfieldBackgroundRenderer(Context context) {
        mContext = context;
        initialize();
    }

    public void drawBackground(Canvas canvas, int left, int top, int right, int bottom, long seed) {
        if (mBgStars == null || mBgStars.isEmpty()) {
            return;
        }

        Rect src, dest;
        Random r = new Random(seed);

        src = new Rect(0, 0, 512, 512);
        dest = new Rect(left, top, right, bottom);
        canvas.drawBitmap(mBgStars.get(r.nextInt(mBgStars.size())), src, dest, mBackgroundPaint);

        for (int i = 0; i < 10; i++) {
            Bitmap gas = mBgGases.get(r.nextInt(mBgGases.size()));

            src = new Rect(0, 0, gas.getWidth(), gas.getHeight());
            int x = left + r.nextInt(right - left + 256) - 128;
            int y = top + r.nextInt(bottom - top + 256) - 128;
            dest = new Rect(x, y, x + (src.width() * 2), y + (src.height() * 2));

            canvas.drawBitmap(gas, src, dest, mBackgroundPaint);
        }
    }

    private void initialize() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStyle(Style.STROKE);
        mBackgroundPaint.setARGB(255, 255, 255, 255);

        AssetManager assetMgr = mContext.getAssets();
        mBgStars = loadBitmaps(assetMgr, "decoration/starfield");
        mBgGases = loadBitmaps(assetMgr, "decoration/gas");
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
