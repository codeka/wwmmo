package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.common.Log;

/** Represents a texture image. */
public class BitmapTexture extends Texture {
  private static final Log log = new Log("TextureBitmap");

  @Nullable private Loader loader;

  private BitmapTexture(@NonNull Loader loader) {
    this.loader = loader;
    this.loader.load();
  }

  @Override
  public void bind() {
    if (loader != null && loader.isLoaded()) {
      setTextureId(loader.createGlTexture());
      loader = null;
    }

    super.bind();
  }

  public static BitmapTexture load(Context context, String fileName) {
    return new BitmapTexture(new Loader(context, fileName, null /* url */));
  }

  public static BitmapTexture loadUrl(Context context, String url) {
    return new BitmapTexture(new Loader(context, null /* fileName */, url));
  }

  /**
   * Handles loading a texture into a {@link BitmapTexture}.
   */
  private static class Loader {
    private Context context;
    private String fileName;
    private String url;
    private Bitmap bitmap;

    Loader(Context context, @Nullable String fileName, @Nullable String url) {
      this.context = Preconditions.checkNotNull(context);
      Preconditions.checkState(fileName != null || url != null);
      this.fileName = fileName;
      this.url = url;
    }

    public void load() {
      if (fileName != null) {
        App.i.getTaskRunner().runTask(() -> {
          try {
            log.info("Loading resource: %s", fileName);
            InputStream ins = context.getAssets().open(fileName);
            bitmap = BitmapFactory.decodeStream(ins);
          } catch (IOException e) {
            log.error("Error loading texture '%s'", fileName, e);
          }
        }, Threads.BACKGROUND);
      } else {
        App.i.getTaskRunner().runTask(
            () -> Picasso.get().load(url).into(picassoTarget),
            Threads.UI);
      }
    }

    boolean isLoaded() {
      return bitmap != null;
    }

    int createGlTexture() {
      Preconditions.checkState(bitmap != null);

      final int[] textureHandleBuffer = new int[1];
      GLES20.glGenTextures(1, textureHandleBuffer, 0);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandleBuffer[0]);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

      return textureHandleBuffer[0];
    }

    /**
     * This is our callback for when Picasso finishes loading an image.
     *
     * We need to keep a strong reference to this, otherwise it gets GC'd before Picasso returns.
     */
    private final Target picassoTarget = new Target() {
      @Override
      public void onBitmapLoaded(Bitmap loadedBitmap, Picasso.LoadedFrom from) {
        bitmap = loadedBitmap;
      }

      @Override
      public void onBitmapFailed(Exception e, Drawable errorDrawable) {
        log.warning("Error loading bitmap: %s", url, e);
      }

      @Override
      public void onPrepareLoad(Drawable placeHolderDrawable) {
      }
    };
  }
}
