package au.com.codeka.warworlds.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.concurrency.Threads;

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
    return new BitmapTexture(new Loader(context, fileName));
  }

  public static BitmapTexture load(Context context, Bitmap bmp) {
    return new BitmapTexture(new Loader(bmp));
  }

  /**
   * Handles loading a texture into a {@link BitmapTexture}.
   */
  private static class Loader {
    private Context context;
    private String fileName;
    private Bitmap bitmap;

    Loader(Context context, @Nullable String fileName) {
      this.context = Preconditions.checkNotNull(context);
      this.fileName = Preconditions.checkNotNull(fileName);
    }

    Loader(Bitmap bmp) {
      this.bitmap = bmp;
    }

    public void load() {
      if (bitmap != null) {
        // We already have a bitmap, nothing to do.
        return;
      }

      App.i.getTaskRunner().runTask(() -> {
        try {
          log.info("Loading resource: %s", fileName);
          InputStream ins = context.getAssets().open(fileName);
          bitmap = BitmapFactory.decodeStream(ins);
        } catch (IOException e) {
          log.error("Error loading texture '%s'", fileName, e);
        }
      }, Threads.BACKGROUND);
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
  }
}
