package au.com.codeka.warworlds.client.opengl;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * {@link TextureManager} manages {@link BitmapTexture}s that we have loaded and ensures we never
 * load the same texture more than once.
 */
public class TextureManager {
  private final Context context;
  private final HashMap<String, WeakReference<BitmapTexture>> cache = new HashMap<>();

  public TextureManager(Context context) {
    this.context = Preconditions.checkNotNull(context);
  }

  /** Loads the texture with the given name, or returns null if the texture could not be loaded. */
  @Nullable
  public BitmapTexture loadTexture(String name) {
    WeakReference<BitmapTexture> ref = cache.get(name);
    if (ref != null) {
      BitmapTexture bmp = ref.get();
      if (bmp != null) {
        return bmp;
      }
    }

    BitmapTexture bmp = BitmapTexture.load(context, name);
    cache.put(name, new WeakReference<>(bmp));
    return bmp;
  }

  /** Loads a texture from the given URL. */
  public BitmapTexture loadTextureUrl(String url) {
    WeakReference<BitmapTexture> ref = cache.get(url);
    if (ref != null) {
      BitmapTexture bmp = ref.get();
      if (bmp != null) {
        return bmp;
      }
    }

    BitmapTexture bmp = BitmapTexture.loadUrl(context, url);
    cache.put(url, new WeakReference<>(bmp));
    return bmp;
  }
}
