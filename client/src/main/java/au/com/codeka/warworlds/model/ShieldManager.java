package au.com.codeka.warworlds.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.ITextureAtlas.ITextureAtlasStateListener;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.source.FileBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.region.ITextureRegion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.util.LruCache;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventBus;

/**
 * This is the base class for the EmpireShieldManager and AllianceShieldManager.
 */
public abstract class ShieldManager implements RealmManager.RealmChangedHandler {
  private static final Log log = new Log("ShieldManager");

  public static final EventBus eventBus = new EventBus();

  // make the bitmap LruCache 2MB maximum
  private final LruCache<Integer, Bitmap> shields = new LruCache<Integer, Bitmap>(2 * 1024 * 1024) {
    @Override
    protected int sizeOf(Integer key, Bitmap value) {
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
        return value.getByteCount();
      } else {
        return value.getHeight() * value.getRowBytes();
      }
    }
  };

  private final LruCache<Integer, ITextureRegion> shieldTextures = new LruCache<>(32);
  private final Set<Integer> fetchingShields = new HashSet<>();

  public static final String EmpireShield = "empire";
  public static final String AllianceShield = "alliance";

  protected ShieldManager() {
    RealmManager.i.addRealmChangedHandler(this);
  }

  public void clearCache() {
    shieldTextures.evictAll();
    shields.evictAll();
  }

  public void clearTextureCache() {
    shieldTextures.evictAll();
  }

  @Override
  public void onRealmChanged(Realm newRealm) {
    clearCache();
  }

  /**
   * Gets (or creates, if there isn't one) the {@link Bitmap} that represents this Empire or
   * Alliance's shield (i.e. their icon).
   */
  protected Bitmap getShield(Context context, ShieldInfo shieldInfo) {
    Bitmap bmp = shields.get(shieldInfo.id);

    if (bmp == null) {
      bmp = loadCachedShieldImage(context, shieldInfo);
      if (bmp == null && shieldInfo.lastUpdate != null) {
        log.info("Getting shield image for %s [id=%d last_update=%d]", shieldInfo.kind,
            shieldInfo.id, shieldInfo.lastUpdate);
        queueFetchShieldImage(shieldInfo);
      }
      if (bmp == null) {
        bmp = getDefaultShield(shieldInfo);
        saveCachedShieldImage(context, shieldInfo, bmp);
      }

      shields.put(shieldInfo.id, bmp);
    }

    return bmp;
  }

  /** Gets (or creates) the empire's shield as an andengine texture. */
  protected ITextureRegion getShieldTexture(BaseGlActivity glActivity, ShieldInfo shieldInfo) {
    ITextureRegion textureRegion = shieldTextures.get(shieldInfo.id);
    if (textureRegion == null) {
      String fullPath = getCacheFile(glActivity, shieldInfo);
      File f = new File(fullPath);
      if (!f.exists()) {
        if (shieldInfo.lastUpdate != null) {
          log.info("Getting shield image for %s [id=%d last_update=%d]", shieldInfo.kind,
              shieldInfo.id, shieldInfo.lastUpdate);
          queueFetchShieldImage(shieldInfo);
        }

        Bitmap bmp = getDefaultShield(shieldInfo);
        saveCachedShieldImage(glActivity, shieldInfo, bmp);
      }

      BitmapTextureAtlas atlas = new BitmapTextureAtlas(glActivity.getTextureManager(), 128, 128,
          TextureOptions.BILINEAR_PREMULTIPLYALPHA);
      atlas.setTextureAtlasStateListener(
          new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());
      textureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromSource(
          atlas, FileBitmapTextureAtlasSource.create(f), 0, 0, 1, 1);
      glActivity.getTextureManager().loadTexture(atlas);

      shieldTextures.put(shieldInfo.id, textureRegion);
    }

    return textureRegion;
  }

  protected abstract Bitmap getDefaultShield(ShieldInfo shieldInfo);

  protected abstract String getFetchUrl(ShieldInfo shieldInfo);

  private void queueFetchShieldImage(final ShieldInfo shieldInfo) {
    log.debug("Queuing up a request to fetch a new shield...");
    synchronized (fetchingShields) {
      if (fetchingShields.contains(shieldInfo.id)) {
        return;
      }
      fetchingShields.add(shieldInfo.id);
    }

    new BackgroundRunner<Bitmap>() {
      @Override
      protected Bitmap doInBackground() {
        String url = getFetchUrl(shieldInfo);
        try {
          Bitmap bmp = ApiClient.getImage(url);

          // save the cached version
          saveCachedShieldImage(App.i, shieldInfo, bmp);
          shields.put(shieldInfo.id, bmp);

          // TODO: fix it
          shieldTextures.remove(shieldInfo.id);

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

        eventBus.publish(new ShieldUpdatedEvent(shieldInfo.kind, shieldInfo.id));
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
        try {
          fos.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private String getCacheFile(Context context, ShieldInfo shieldInfo) {
    long lastUpdate = 0;
    if (shieldInfo.lastUpdate != null) {
      lastUpdate = shieldInfo.lastUpdate;
    }

    File cacheDir = context.getCacheDir();
    String fullPath = cacheDir.getAbsolutePath() + File.separator + shieldInfo.kind
        + "-shields" + File.separator;
    fullPath += String.format("%d-v3-%d-%d.png", RealmContext.i.getCurrentRealm().getID(),
        shieldInfo.id, lastUpdate);
    return fullPath;
  }

  protected class ShieldInfo {
    public String kind;
    public int id;
    public Long lastUpdate;

    public ShieldInfo(String kind, int id, Long lastUpdate) {
      this.kind = kind;
      this.id = id;
      this.lastUpdate = lastUpdate;
    }
  }

  public static class ShieldUpdatedEvent {
    public String kind;
    public int id;

    public ShieldUpdatedEvent(String kind, int id) {
      this.kind = kind;
      this.id = id;
    }
  }
}
