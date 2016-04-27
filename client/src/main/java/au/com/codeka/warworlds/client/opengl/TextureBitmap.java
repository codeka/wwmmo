package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.warworlds.common.Log;

/** Represents a texture image. */
public class TextureBitmap {
  private static final Log log = new Log("TextureBitmap");

  private int id;

  public TextureBitmap(int id) {
    this.id = id;
  }

  public void bind() {
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
  }

  @Nullable
  public static TextureBitmap load(Context context, String fileName) {
    final int[] textureHandleBuffer = new int[1];
    GLES20.glGenTextures(1, textureHandleBuffer, 0);

    try {
      InputStream ins = context.getAssets().open(fileName);
      Bitmap bmp = BitmapFactory.decodeStream(ins);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandleBuffer[0]);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
    } catch (IOException e) {
      GLES20.glDeleteBuffers(1, textureHandleBuffer, 0);
      log.warning("Error loading texture '%s'", fileName, e);
      return null;
    }

    return new TextureBitmap(textureHandleBuffer[0]);
  }
}
