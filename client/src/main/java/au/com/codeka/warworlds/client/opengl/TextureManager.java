package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * {@link TextureManager} manages {@link TextureBitmap}s that we have loaded and ensures we never
 * load the same texture more than once.
 */
public class TextureManager {
  private final Context context;
  private final HashMap<String, WeakReference<TextureBitmap>> cache = new HashMap<>();

  public TextureManager(Context context) {
    this.context = Preconditions.checkNotNull(context);
  }

  /** Loads the texture with the given name, or returns null if the texture could not be loaded. */
  @Nullable
  public TextureBitmap loadTexture(String name) {
    WeakReference<TextureBitmap> ref = cache.get(name);
    if (ref != null) {
      TextureBitmap bmp = ref.get();
      if (bmp != null) {
        return bmp;
      }
    }

    TextureBitmap bmp = TextureBitmap.load(context, name);
    cache.put(name, new WeakReference<>(bmp));
    return bmp;
  }
}
