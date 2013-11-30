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

import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.ITextureAtlas.ITextureAtlasStateListener;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.source.FileBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

public class EmpireShieldManager {
    private static final Logger log = LoggerFactory.getLogger(EmpireShieldManager.class);
    public static EmpireShieldManager i = new EmpireShieldManager();

    private Bitmap sBaseShield;
    private Map<String, SoftReference<Bitmap>> mEmpireShields;
    private Map<String, ITextureRegion> mEmpireShieldTextures;
    private List<EmpireShieldUpdatedHandler> mEmpireShieldUpdatedHandlers;
    private Set<Integer> mFetchingShields;

    private EmpireShieldManager() {
        mEmpireShields = new HashMap<String, SoftReference<Bitmap>>();
        mEmpireShieldTextures = new HashMap<String, ITextureRegion>();
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
            bmp = loadCachedShieldImage(context, empire);
            if (bmp == null && empire.getShieldLastUpdate() != null) {
                log.info(String.format("Getting shield image for empire [key=%s last_update=%s]",
                        empire.getKey(), empire.getShieldLastUpdate()));
                queueFetchShieldImage(empire);
            }
            if (bmp == null) {
                int colour = getShieldColour(empire);
                bmp = combineShieldColour(context, colour);

                saveCachedShieldImage(context, empire, bmp);
            }

            mEmpireShields.put(empire.getKey(), new SoftReference<Bitmap>(bmp));
        }

        return bmp;
    }

    public void clearTextureCache() {
        mEmpireShieldTextures.clear();
    }

    /** Gets (or creates) the empire's shield as an andengine texture. */
    public ITextureRegion getShieldTexture(BaseGlActivity glActivity, Empire empire) {
        ITextureRegion textureRegion = mEmpireShieldTextures.get(empire.getKey());
        if (textureRegion == null) {
            String fullPath = getCacheFile(glActivity, empire);
            File f = new File(fullPath);
            if (!f.exists()) {
                if (empire.getShieldLastUpdate() != null) {
                    log.info(String.format("Getting shield image for empire [key=%s last_update=%s]",
                            empire.getKey(), empire.getShieldLastUpdate()));
                    queueFetchShieldImage(empire);
                }

                int colour = getShieldColour(empire);
                Bitmap bmp = combineShieldColour(glActivity, colour);
                saveCachedShieldImage(glActivity, empire, bmp);
            }

            BitmapTextureAtlas atlas = new BitmapTextureAtlas(glActivity.getTextureManager(), 128, 128,
                    TextureOptions.BILINEAR_PREMULTIPLYALPHA);
            atlas.setTextureAtlasStateListener(new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());
            textureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromSource(
                    atlas, FileBitmapTextureAtlasSource.create(f), 0, 0, 1, 1);
            glActivity.getTextureManager().loadTexture(atlas);

            mEmpireShieldTextures.put(empire.getKey(), textureRegion);
        }

        return textureRegion;
    }

    private void queueFetchShieldImage(final Empire empire) {
        log.debug("queuing up a request to fetch a new shield...");
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
                    bmp = combineShieldImage(App.i, bmp);

                    // save the cached version
                    log.debug("got a new shield image for "+empireID);
                    saveCachedShieldImage(App.i, empire, bmp);
                    mEmpireShields.put(empire.getKey(), new SoftReference<Bitmap>(bmp));

                    // TODO: fix it
                    mEmpireShieldTextures.remove(empireID);

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

    private Bitmap loadCachedShieldImage(Context context, Empire empire) {
        String fullPath = getCacheFile(context, empire);
        File f = new File(fullPath);
        if (!f.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(fullPath);
    }

    private void saveCachedShieldImage(Context context, Empire empire, Bitmap bmp) {
        String fullPath = getCacheFile(context, empire);

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

    private String getCacheFile(Context context, Empire empire) {
        long lastUpdate = 0;
        if (empire.getShieldLastUpdate() != null) {
            lastUpdate = empire.getShieldLastUpdate().getMillis();
        }

        File cacheDir = context.getCacheDir();
        String fullPath = cacheDir.getAbsolutePath() + File.separator + "empire-shields" + File.separator;
        fullPath += String.format("%d-v2-%s-%d.png", RealmContext.i.getCurrentRealm().getID(), empire.getKey(), lastUpdate);
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
