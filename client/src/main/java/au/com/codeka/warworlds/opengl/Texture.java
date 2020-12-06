package au.com.codeka.warworlds.client.opengl;

import android.opengl.GLES20;

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
    } else {
      // Binding 0 means we'll get a black texture, but it's better than whatever random texture
      // was bound before. TODO: bind an explicitly transparent texture?
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
  }
}
