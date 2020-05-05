package au.com.codeka.warworlds.client.opengl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.opengl.GLES20
import android.opengl.GLUtils
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.common.Log
import com.google.common.base.Preconditions
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.io.IOException

/** Represents a texture image.  */
class BitmapTexture private constructor(loader: Loader) : Texture() {
  private var loader: Loader?

  override fun bind() {
    val l = loader
    if (l != null && l.isLoaded) {
      setTextureId(l.createGlTexture())
      loader = null
    }
    super.bind()
  }

  /**
   * Handles loading a texture into a [BitmapTexture].
   */
  private class Loader internal constructor(private val context: Context, fileName: String?, url: String?) {
    private val fileName: String?
    private val url: String?
    private var bitmap: Bitmap? = null

    fun load() {
      if (fileName != null) {
        App.taskRunner.runTask(Runnable {
          try {
            log.info("Loading resource: %s", fileName)
            val ins = context.assets.open(fileName)
            bitmap = BitmapFactory.decodeStream(ins)
          } catch (e: IOException) {
            log.error("Error loading texture '%s'", fileName, e)
          }
        }, Threads.BACKGROUND)
      } else {
        App.taskRunner.runTask(
            Runnable { Picasso.get().load(url).into(picassoTarget) },
            Threads.UI)
      }
    }

    val isLoaded: Boolean
      get() = bitmap != null

    fun createGlTexture(): Int {
      Preconditions.checkState(bitmap != null)
      val textureHandleBuffer = IntArray(1)
      GLES20.glGenTextures(1, textureHandleBuffer, 0)
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandleBuffer[0])
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
      return textureHandleBuffer[0]
    }

    /**
     * This is our callback for when Picasso finishes loading an image.
     *
     * We need to keep a strong reference to this, otherwise it gets GC'd before Picasso returns.
     */
    val picassoTarget: Target = object : Target {
      override fun onBitmapLoaded(loadedBitmap: Bitmap, from: LoadedFrom) {
        bitmap = loadedBitmap
      }

      override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
        log.warning("error loading bitmap: %s", url, e)
      }

      override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
    }

    init {
      Preconditions.checkState(fileName != null || url != null)
      this.fileName = fileName
      this.url = url
    }
  }

  companion object {
    private val log = Log("TextureBitmap")
    @JvmStatic
    fun load(context: Context, fileName: String?): BitmapTexture {
      return BitmapTexture(Loader(context, fileName, null /* url */))
    }

    @JvmStatic
    fun loadUrl(context: Context, url: String?): BitmapTexture {
      return BitmapTexture(Loader(context, null /* fileName */, url))
    }
  }

  init {
    loader.load()
    this.loader = loader
  }
}