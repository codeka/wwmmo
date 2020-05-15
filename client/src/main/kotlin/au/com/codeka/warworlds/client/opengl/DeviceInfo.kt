package au.com.codeka.warworlds.client.opengl

import android.opengl.GLES20
import au.com.codeka.warworlds.client.BuildConfig
import au.com.codeka.warworlds.common.Log

/** Contains some useful info about the current OpenGL renderer. */
class DeviceInfo {
  companion object {
    private val log = Log("DeviceInfo")
  }

  private val version: String = GLES20.glGetString(GLES20.GL_VERSION)
  private val renderer: String = GLES20.glGetString(GLES20.GL_RENDERER)
  private val extensions: String = GLES20.glGetString(GLES20.GL_EXTENSIONS)
  private val maxTextureUnits: Int
  private val maxTextureSize: Int
  private val getIntegerBuffer = IntArray(1)

  private fun getInteger(attr: Int): Int {
    GLES20.glGetIntegerv(attr, getIntegerBuffer, 0)
    return getIntegerBuffer[0]
  }

  init {
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