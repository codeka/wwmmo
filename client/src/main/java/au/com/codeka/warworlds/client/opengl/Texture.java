package au.com.codeka.warworlds.client.opengl;

import android.opengl.GLES20;

import com.google.common.base.Preconditions;

/**
 * Base class for all textures in the app.
 */
public class Texture {
  private int id;

  protected Texture() {
    id = -1;
  }

  protected void setTextureId(int id) {
    this.id = id;
  }

  public void bind() {
    if (id != -1) {
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
    }
  }
}
