package au.com.codeka.warworlds.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

public class EmpireShieldManager {
    private static final Logger log = LoggerFactory.getLogger(EmpireShieldManager.class);
    public static EmpireShieldManager i = new EmpireShieldManager();

    private Bitmap sBaseShield;
    private Map<String, SoftReference<Bitmap>> mEmpireShields;
    private List<EmpireShieldUpdatedHandler> mEmpireShieldUpdatedHandlers;
    private Set<Integer> mFetchingShields;

    private EmpireShieldManager() {
        mEmpireShields = new HashMap<String, SoftReference<Bitmap>>();
        mEmpireShieldUpdatedHandlers = new ArrayList<EmpireShieldUpdatedHandler>();
        mFetchingShields = new HashSet<Integer>();
    }

    public void addEmpireShieldUpdatedHandler(EmpireShieldUpdatedHandler handler) {
        synchronized(mEmpireShieldUpdatedHandlers) {
            mEmpireShieldUpdatedHandlers.add(handler);
        }
    }

    public void removeEmpireShieldUpdatedHandler(EmpireShieldUpdatedHandler handler) {
        synchronized(mEmpireShieldUpdatedHandlers) {
            mEmpireShieldUpdatedHandlers.remove(handler);
        }
    }

    protected void fireEmpireShieldUpdatedHandler(int empireID) {
        synchronized(mEmpireShieldUpdatedHandlers) {
            for(EmpireShieldUpdatedHandler handler : mEmpireShieldUpdatedHandlers) {
                handler.onEmpireShieldUpdated(empireID);
            }
        }
    }

    public void flushCachedImage(String empireKey) {
        mEmpireShields.remove(empireKey);
    }

    /**
     * Gets (or creates, if there isn't one) the \c Bitmap that represents this Empire's
     * shield (i.e. their icon).
     */
    public Bitmap getShield(Context context, Empire empire) {
        Bitmap bmp = null;
        SoftReference<Bitmap> bmpref = mEmpireShields.get(empire.getKey());
        if (bmpref != null) {
            bmp = bmpref.get();
        }

        if (bmp == null) {
            if (empire.getShieldLastUpdate() != null) {
                log.info(String.format("Getting shield image for empire [key=%s last_update=%s]",
                        empire.getKey(), empire.getShieldLastUpdate()));
                Bitmap shieldBitmap = loadCachedShieldImage(context, Integer.parseInt(empire.getKey()),
                        empire.getShieldLastUpdate().getMillis());
                if (shieldBitmap == null) {
                    queueFetchShieldImage(empire);
                } else {
                    bmp = combineShieldImage(context, shieldBitmap);
                }
            }

            if (bmp == null) {
                int colour = getShieldColour(empire);
                bmp = combineShieldColour(context, colour);
            }

            mEmpireShields.put(empire.getKey(), new SoftReference<Bitmap>(bmp));
        }

        return bmp;
    }

    private void queueFetchShieldImage(final Empire empire) {
        final int empireID = Integer.parseInt(empire.getKey());
        synchronized(mFetchingShields) {
            if (mFetchingShields.contains(empireID)) {
                return;
            }
            mFetchingShields.add(empireID);
        }

        new BackgroundRunner<Bitmap>() {
            @Override
            protected Bitmap doInBackground() {
                String url = "empires/"+empireID+"/shield";
                try {
                    Bitmap bmp = ApiClient.getImage(url);

                    // save the cached version
                    saveCachedShieldImage(App.i, empireID, empire.getShieldLastUpdate().getMillis(), bmp);

                    // re-generate the in-memory cache
                    bmp = combineShieldImage(App.i, bmp);
                    mEmpireShields.put(empire.getKey(), new SoftReference<Bitmap>(bmp));

                    return bmp;
                } catch (ApiException e) {
                    return null;
                }
            }

            @Override
            protected void onComplete(Bitmap bmp) {
                if (bmp == null) {
                    return; // TODO: handle errors
                }

                fireEmpireShieldUpdatedHandler(empireID);
            }
        }.execute();
    }

    /**
     * Combines the base shield image with the given colour.
     */
    public Bitmap combineShieldColour(Context context, int colour) {
        ensureBaseImage(context);

        int width = sBaseShield.getWidth();
        int height = sBaseShield.getHeight();
        int[] pixels = new int[width * height];
        sBaseShield.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == Color.MAGENTA) {
                pixels[i] = colour;
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    /**
     * Combines the given image with the base shield image.
     */
    public Bitmap combineShieldImage(Context context, Bitmap otherImage) {
        ensureBaseImage(context);

        int width = sBaseShield.getWidth();
        int height = sBaseShield.getHeight();
        int[] pixels = new int[width * height];
        sBaseShield.getPixels(pixels, 0, width, 0, 0, width, height);

        float sx = (float) otherImage.getWidth() / (float) width;
        float sy = (float) otherImage.getHeight() / (float) height;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == Color.MAGENTA) {
                int y = i / width;
                int x = i % width;
                pixels[i] = otherImage.getPixel((int)(x * sx), (int)(y * sy));
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    public int getShieldColour(Empire empire) {
        if (empire.getKey() == null) {
            return Color.TRANSPARENT;
        }

        Random rand = new Random(empire.getKey().hashCode());
        return Color.rgb(rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100);
    }

    public float[] getShieldColourFloats(Empire empire) {
        if (empire.getKey() == null) {
            return new float[] {0.0f, 0.0f, 0.0f, 0.0f};
        }

        Random rand = new Random(empire.getKey().hashCode());
        return new float[] {((float) rand.nextInt(100) + 100) / 256.0f,
                            ((float) rand.nextInt(100) + 100) / 256.0f,
                            ((float) rand.nextInt(100) + 100) / 256.0f,
                            1.0f};
    }

    private Bitmap loadCachedShieldImage(Context context, int empireID, long lastUpdateTime) {
        String fullPath = getCacheFile(context, empireID, lastUpdateTime);
        File f = new File(fullPath);
        if (!f.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(fullPath);
    }

    private void saveCachedShieldImage(Context context, int empireID, long lastUpdateTime, Bitmap bmp) {
        String fullPath = getCacheFile(context, empireID, lastUpdateTime);

        // make sure the directory exists...
        File f = new File(fullPath);
        f = f.getParentFile();
        f.mkdirs();

        try {
            bmp.compress(CompressFormat.PNG, 100, new FileOutputStream(fullPath));
        } catch (FileNotFoundException e) {
            // ignore errors
        }
    }

    private String getCacheFile(Context context, int empireID, long lastUpdateTime) {
        File cacheDir = context.getCacheDir();
        String fullPath = cacheDir.getAbsolutePath() + File.separator + "empire-shields" + File.separator;
        fullPath += String.format("%d-%d.png", empireID, lastUpdateTime);
        return fullPath;
    }

    private void ensureBaseImage(Context context) {
        if (sBaseShield == null) {
            AssetManager assetManager = context.getAssets();
            InputStream ins;
            try {
                ins = assetManager.open("img/shield.png");
            } catch (IOException e) {
                // should never happen!
                return;
            }

            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPurgeable = true;
                opts.inInputShareable = true;
                sBaseShield = BitmapFactory.decodeStream(ins, null, opts);
            } finally {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public interface EmpireShieldUpdatedHandler {
        void onEmpireShieldUpdated(int empireID);
    }
}
