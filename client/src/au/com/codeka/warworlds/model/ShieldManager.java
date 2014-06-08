package au.com.codeka.warworlds.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

/**
 * This is the base class for the EmpireShieldManager and AllianceShieldManager.
 */
public abstract class ShieldManager implements RealmManager.RealmChangedHandler {
    private static final Logger log = LoggerFactory.getLogger(ShieldManager.class);

    private SparseArray<SoftReference<Bitmap>> mShields;
    private SparseArray<ITextureRegion> mShieldTextures;
    private List<ShieldUpdatedHandler> mShieldUpdatedHandlers;
    private Set<Integer> mFetchingShields;

    protected ShieldManager() {
        mShields = new SparseArray<SoftReference<Bitmap>>();
        mShieldTextures = new SparseArray<ITextureRegion>();
        mShieldUpdatedHandlers = new ArrayList<ShieldUpdatedHandler>();
        mFetchingShields = new HashSet<Integer>();

        RealmManager.i.addRealmChangedHandler(this);
    }

    public void addShieldUpdatedHandler(ShieldUpdatedHandler handler) {
        synchronized(mShieldUpdatedHandlers) {
            mShieldUpdatedHandlers.add(handler);
        }
    }

    public void removeShieldUpdatedHandler(ShieldUpdatedHandler handler) {
        synchronized(mShieldUpdatedHandlers) {
            mShieldUpdatedHandlers.remove(handler);
        }
    }

    protected void fireShieldUpdatedHandler(int id) {
        synchronized(mShieldUpdatedHandlers) {
            for(ShieldUpdatedHandler handler : mShieldUpdatedHandlers) {
                handler.onShieldUpdated(id);
            }
        }
    }

    public void flushCachedImage(int id) {
        mShields.remove(id);
    }

    public void clearCache() {
        mShieldTextures.clear();
        mShields.clear();
    }

    @Override
    public void onRealmChanged(Realm newRealm) {
        clearCache();
    }

    /**
     * Gets (or creates, if there isn't one) the \c Bitmap that represents this Empire or
     * Alliance's shield (i.e. their icon).
     */
    protected Bitmap getShield(Context context, ShieldInfo shieldInfo) {
        Bitmap bmp = null;
        SoftReference<Bitmap> bmpref = mShields.get(shieldInfo.id);
        if (bmpref != null) {
            bmp = bmpref.get();
        }

        if (bmp == null) {
            bmp = loadCachedShieldImage(context, shieldInfo);
            if (bmp == null && shieldInfo.lastUpdate != null) {
                log.info(String.format("Getting shield image for %s [id=%d last_update=%d]",
                        shieldInfo.kind, shieldInfo.id, shieldInfo.lastUpdate));
                queueFetchShieldImage(shieldInfo);
            }
            if (bmp == null) {
                bmp = getDefaultShield(shieldInfo);

                saveCachedShieldImage(context, shieldInfo, bmp);
            }

            mShields.put(shieldInfo.id, new SoftReference<Bitmap>(bmp));
        }

        return bmp;
    }

    public void clearTextureCache() {
        mShieldTextures.clear();
    }

    /** Gets (or creates) the empire's shield as an andengine texture. */
    protected ITextureRegion getShieldTexture(BaseGlActivity glActivity, ShieldInfo shieldInfo) {
        ITextureRegion textureRegion = mShieldTextures.get(shieldInfo.id);
        if (textureRegion == null) {
            String fullPath = getCacheFile(glActivity, shieldInfo);
            File f = new File(fullPath);
            if (!f.exists()) {
                if (shieldInfo.lastUpdate != null) {
                    log.info(String.format("Getting shield image for %s [id=%d last_update=%d]",
                            shieldInfo.kind, shieldInfo.id, shieldInfo.lastUpdate));
                    queueFetchShieldImage(shieldInfo);
                }

                Bitmap bmp = getDefaultShield(shieldInfo);
                saveCachedShieldImage(glActivity, shieldInfo, bmp);
            }

            BitmapTextureAtlas atlas = new BitmapTextureAtlas(glActivity.getTextureManager(), 128, 128,
                    TextureOptions.BILINEAR_PREMULTIPLYALPHA);
            atlas.setTextureAtlasStateListener(new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());
            textureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromSource(
                    atlas, FileBitmapTextureAtlasSource.create(f), 0, 0, 1, 1);
            glActivity.getTextureManager().loadTexture(atlas);

            mShieldTextures.put(shieldInfo.id, textureRegion);
        }

        return textureRegion;
    }

    protected abstract Bitmap processShieldImage(Bitmap bmp);
    protected abstract Bitmap getDefaultShield(ShieldInfo shieldInfo);
    protected abstract String getFetchUrl(ShieldInfo shieldInfo);

    private void queueFetchShieldImage(final ShieldInfo shieldInfo) {
        log.debug("queuing up a request to fetch a new shield...");
        synchronized(mFetchingShields) {
            if (mFetchingShields.contains(shieldInfo.id)) {
                return;
            }
            mFetchingShields.add(shieldInfo.id);
        }

        new BackgroundRunner<Bitmap>() {
            @Override
            protected Bitmap doInBackground() {
                String url = getFetchUrl(shieldInfo);
                try {
                    Bitmap bmp = ApiClient.getImage(url);
                    bmp = processShieldImage(bmp);

                    // save the cached version
                    saveCachedShieldImage(App.i, shieldInfo, bmp);
                    mShields.put(shieldInfo.id, new SoftReference<Bitmap>(bmp));

                    // TODO: fix it
                    mShieldTextures.remove(shieldInfo.id);

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

                fireShieldUpdatedHandler(shieldInfo.id);
            }
        }.execute();
    }

    private Bitmap loadCachedShieldImage(Context context, ShieldInfo shieldInfo) {
        String fullPath = getCacheFile(context, shieldInfo);
        File f = new File(fullPath);
        if (!f.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(fullPath);
    }

    private void saveCachedShieldImage(Context context, ShieldInfo shieldInfo, Bitmap bmp) {
        String fullPath = getCacheFile(context, shieldInfo);

        // make sure the directory exists...
        File f = new File(fullPath);
        f = f.getParentFile();
        f.mkdirs();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fullPath);
            bmp.compress(CompressFormat.PNG, 100, fos);
        } catch (FileNotFoundException e) {
            // ignore errors
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (IOException e) {}
            }
        }
    }

    private String getCacheFile(Context context, ShieldInfo shieldInfo) {
        long lastUpdate = 0;
        if (shieldInfo.lastUpdate != null) {
            lastUpdate = shieldInfo.lastUpdate;
        }

        File cacheDir = context.getCacheDir();
        String fullPath = cacheDir.getAbsolutePath() + File.separator + shieldInfo.kind + "-shields" + File.separator;
        fullPath += String.format("%d-v2-%d-%d.png", RealmContext.i.getCurrentRealm().getID(), shieldInfo.id, lastUpdate);
        return fullPath;
    }

    protected class ShieldInfo
    {
        public String kind;
        public int id;
        public Long lastUpdate;

        public ShieldInfo(String kind, int id, Long lastUpdate) {
            this.kind = kind;
            this.id = id;
            this.lastUpdate = lastUpdate;
        }
    }

    public interface ShieldUpdatedHandler {
        void onShieldUpdated(int id);
    }
}
