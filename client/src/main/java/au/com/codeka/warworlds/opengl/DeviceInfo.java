package au.com.codeka.warworlds.opengl;

import android.opengl.GLES20;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.Util;

/**
 * Contains some useful info about the current OpenGL renderer.
 */
public class DeviceInfo {
  private final static Log log = new Log("DeviceInfo");

  private final String version;
  private final String renderer;
  private final String extensions;

  private final int maxTextureUnits;
  private final int maxTextureSize;

  public DeviceInfo() {
    version = GLES20.glGetString(GLES20.GL_VERSION);
    renderer = GLES20.glGetString(GLES20.GL_RENDERER);
    extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);

    maxTextureUnits = this.getInteger(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS);
    maxTextureSize = this.getInteger(GLES20.GL_MAX_TEXTURE_SIZE);

    if (Util.isDebug()) {
      log.debug("VERSION: %s", version);
      log.debug("RENDERER: %s", renderer);
      log.debug("EXTENSIONS: %s", extensions);
      log.debug("MAX_TEXTURE_IMAGE_UNITS: %d", maxTextureUnits);
      log.debug("MAX_TEXTURE_SIZE: %d", maxTextureSize);
    }
  }

  private final int[] getIntegerBuffer = new int[1];
  public int getInteger(final int attr) {
    GLES20.glGetIntegerv(attr, getIntegerBuffer, 0);
    return getIntegerBuffer[0];
  }
}
