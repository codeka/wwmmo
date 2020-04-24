package au.com.codeka.warworlds.client.opengl

import android.content.Context
import au.com.codeka.warworlds.client.opengl.BitmapTexture.Companion.load
import au.com.codeka.warworlds.client.opengl.BitmapTexture.Companion.loadUrl
import com.google.common.base.Preconditions
import java.lang.ref.WeakReference
import java.util.*

/**
 * [TextureManager] manages [BitmapTexture]s that we have loaded and ensures we never
 * load the same texture more than once.
 */
class TextureManager(private val context: Context) {
  private val cache = HashMap<String, WeakReference<BitmapTexture>>()

  /** Loads the texture with the given name, or returns null if the texture could not be loaded.  */
  fun loadTexture(name: String): BitmapTexture? {
    synchronized(cache) {
      val ref = cache[name]
      if (ref != null) {
        val bmp = ref.get()
        if (bmp != null) {
          return bmp
        }
      }
      val bmp = load(context, name)
      cache[name] = WeakReference(bmp)
      return bmp
    }
  }

  /** Loads a texture from the given URL.  */
  fun loadTextureUrl(url: String): BitmapTexture {
    synchronized(cache) {
      val ref = cache[url]
      if (ref != null) {
        val bmp = ref.get()
        if (bmp != null) {
          return bmp
        }
      }
      val bmp = loadUrl(context, url)
      cache[url] = WeakReference(bmp)
      return bmp
    }
  }
}
