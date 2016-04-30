package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.warworlds.client.concurrency.BackgroundRunner;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.common.Log;

/** Represents a texture image. */
public class TextureBitmap {
  private static final Log log = new Log("TextureBitmap");

  @Nullable private Loader loader;
  private int id;

  private TextureBitmap(@NonNull Loader loader) {
    this.loader = loader;
    this.loader.load();
  }

  public void bind() {
    if (loader != null) {
      if (loader.isLoaded()) {
        id = loader.createGlTexture();
        loader = null;
      }
    }

    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
  }

  @Nullable
  public static TextureBitmap load(Context context, String fileName) {
    return new TextureBitmap(new Loader(context, fileName));
  }

  /**
   * Handles loading a texture into a {@link TextureBitmap}.
   */
  private static class Loader {
    private Context context;
    private String fileName;
    private Bitmap bitmap;

    public Loader(Context context, String fileName) {
      this.context = Preconditions.checkNotNull(context);
      this.fileName = Preconditions.checkNotNull(fileName);
    }

    public void load() {
      // TODO: do this on a background thread
      try {
        InputStream ins = context.getAssets().open(fileName);
        bitmap = BitmapFactory.decodeStream(ins);
      } catch (IOException e) {
        log.warning("Error loading texture '%s'", fileName, e);
      }
    }

    boolean isLoaded() {
      return bitmap != null;
    }

    public int createGlTexture() {
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
