package au.com.codeka.warworlds.client.opengl

import android.opengl.GLES20
import au.com.codeka.warworlds.client.BuildConfig
import au.com.codeka.warworlds.common.Log

/**
 * Contains some useful info about the current OpenGL renderer.
 */
class DeviceInfo {
  private val version: String
  private val renderer: String
  private val extensions: String
  private val maxTextureUnits: Int
  private val maxTextureSize: Int
  private val getIntegerBuffer = IntArray(1)
  fun getInteger(attr: Int): Int {
    GLES20.glGetIntegerv(attr, getIntegerBuffer, 0)
    return getIntegerBuffer[0]
  }

  companion object {
    private val log = Log("DeviceInfo")
  }

  init {
    version = GLES20.glGetString(GLES20.GL_VERSION)
    renderer = GLES20.glGetString(GLES20.GL_RENDERER)
    extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)
    maxTextureUnits = getInteger(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS)
    maxTextureSize = getInteger(GLES20.GL_MAX_TEXTURE_SIZE)
    if (BuildConfig.DEBUG) {
      log.debug("VERSION: %s", version)
      log.debug("RENDERER: %s", renderer)
      log.debug("EXTENSIONS: %s", extensions)
      log.debug("MAX_TEXTURE_IMAGE_UNITS: %d", maxTextureUnits)
      log.debug("MAX_TEXTURE_SIZE: %d", maxTextureSize)
    }
  }
}