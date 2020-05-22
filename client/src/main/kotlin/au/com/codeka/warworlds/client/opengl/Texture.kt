package au.com.codeka.warworlds.client.opengl

import android.opengl.GLES20

/** Base class for all textures in the app. */
open class Texture protected constructor() {
  private var id: Int

  protected fun setTextureId(id: Int) {
    this.id = id
  }

  open fun bind() {
    if (id != -1) {
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
    } else {
      // Binding 0 means we'll get a black texture, but it's better than whatever random texture
      // was bound before. TODO: bind an explicitly transparent texture?
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
  }

  fun close() {
    if (id != -1) {
     val textureHandleBuffer = intArrayOf(id)
      GLES20.glDeleteTextures(1, textureHandleBuffer, 0)
    }
  }

  init {
    id = -1
  }
}