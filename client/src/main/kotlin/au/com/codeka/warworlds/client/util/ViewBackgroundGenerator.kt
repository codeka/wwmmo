package au.com.codeka.warworlds.client.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.common.Log
import com.google.common.base.Preconditions
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Generates an image that can be used at the background for the various non-game activities
 * (such as the startup activity, account activities, etc).
 */
object ViewBackgroundGenerator {
  private val log = Log("BackgroundGenerator")

  /**
   * This is the [Bitmap] which contains the background. We'll create it the first time you
   * set a background, and then keep it and reuse it.
   */
  private var bitmap: Bitmap? = null

  /**
   * The renderer we'll use to render the background.
   */
  private var renderer: BackgroundRenderer? = null

  /**
   * The last seed we used to generate the bitmap. If we're called with the same seed again, we'll
   * just reuse the current bitmap.
   */
  private var lastSeed: Long? = null

  // The scale to apply to the bitmap.
  private var bitmapScale = 0f

  /**
   * Sets the background of the given [View] to our custom bitmap. Must be called on the UI
   * thread.
   *
   * @param view The view to set the background on.
   */
  fun setBackground(view: View) {
    setBackground(view, null as OnDrawHandler?)
  }

  /**
   * Sets the background of the given [View] to our custom bitmap. Must be called on the UI
   * thread.
   *
   * @param view The view to set the background on.
   * @param onDrawHandler An optional [OnDrawHandler] that we'll call when we draw the
   * background.
   */
  fun setBackground(view: View, onDrawHandler: OnDrawHandler?) {
    setBackground(view, onDrawHandler, Random().nextLong())
  }

  /**
   * Sets the background of the given [View] to our custom bitmap. Must be called on the UI
   * thread.
   *
   * @param view The view to set the background on.
   * @param onDrawHandler An optional [OnDrawHandler] that we'll call when we draw the
   * background.
   * @param seed A seed that we use to generate the image. You can use this to ensure the same
   * image gets generated for the same seed.
   */
  @Suppress("deprecation") // We need to work on API level 21.
  fun setBackground(view: View, onDrawHandler: OnDrawHandler?, seed: Long) {
    Preconditions.checkState(Threads.UI.isCurrentThread)
    if (bitmap == null || renderer == null) {
      val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

      val display = wm.defaultDisplay
      val metrics = DisplayMetrics()
      display.getMetrics(metrics)
      bitmapScale = metrics.density
      bitmap = Bitmap.createBitmap(
          (metrics.widthPixels / bitmapScale).toInt(),
          (metrics.heightPixels / bitmapScale).toInt(),
          Bitmap.Config.ARGB_8888)
      renderer = BackgroundRenderer(view.context)
    }
    if (lastSeed == null || seed != lastSeed) {
      renderer!!.render(bitmap, seed)
      lastSeed = seed
    }
    view.background = BackgroundDrawable(bitmap, onDrawHandler)
  }

  interface OnDrawHandler {
    fun onDraw(canvas: Canvas?)
  }

  /** A custom [Drawable] which we use to draw the bitmap to the screen.  */
  class BackgroundDrawable(private val bitmap: Bitmap?, private val onDrawHandler: OnDrawHandler?) : Drawable() {
    private val paint: Paint
    private val matrix: Matrix
    override fun draw(canvas: Canvas) {
      canvas.drawBitmap(bitmap!!, matrix, paint)
      onDrawHandler?.onDraw(canvas)
    }

    override fun getIntrinsicWidth(): Int {
      return bitmap!!.width
    }

    override fun getIntrinsicHeight(): Int {
      return bitmap!!.height
    }

    override fun getOpacity(): Int {
      return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}

    init {
      paint = Paint()
      paint.style = Paint.Style.STROKE
      paint.setARGB(255, 255, 255, 255)
      matrix = Matrix()
      matrix.setScale(bitmapScale, bitmapScale)
    }
  }

  /**
   * This class is responsible for actually rendering the background.
   */
  private class BackgroundRenderer(context: Context) {
    private var backgroundPaint: Paint? = null
    private var starfieldBitmap: Bitmap? = null
    private var gasBitmap: Bitmap? = null

    /**
     * Renders the background to the given bitmap, which we can then use to render the background
     * again later.
     */
    fun render(bmp: Bitmap?, seed: Long) {
      // start off black
      val canvas = Canvas(bmp!!)
      canvas.drawColor(Color.BLACK)
      var src: Rect
      var dest: Rect
      val r = Random(seed)
      backgroundPaint = Paint()
      backgroundPaint!!.style = Paint.Style.STROKE
      backgroundPaint!!.setARGB(255, 255, 255, 255)
      src = Rect(0, 0, starfieldBitmap!!.width, starfieldBitmap!!.height)
      var x = 0
      while (x < bmp.width) {
        var y = 0
        while (y < bmp.height) {
          dest = Rect(x, y, x + src.width(), y + src.height())
          canvas.drawBitmap(starfieldBitmap!!, src, dest, backgroundPaint)
          y += src.height()
        }
        x += src.width()
      }
      val gasSize = gasBitmap!!.width / 4
      for (i in 0..9) {
        var dx = r.nextInt(4) * gasSize
        var dy = r.nextInt(4) * gasSize
        src = Rect(dx, dy, dx + gasSize, dy + gasSize)
        dx = r.nextInt(canvas.width) - src.width()
        dy = r.nextInt(canvas.height) - src.height()
        dest = Rect(dx, dy, dx + src.width() * 2, dy + src.height() * 2)
        canvas.drawBitmap(gasBitmap!!, src, dest, backgroundPaint)
      }
    }

    /** Loads all bitmaps from a given asset subfolder into an array.  */
    private fun loadBitmap(assetMgr: AssetManager, path: String): Bitmap? {
      var bitmap: Bitmap? = null
      var ins: InputStream? = null
      try {
        ins = assetMgr.open(path)
        bitmap = BitmapFactory.decodeStream(ins, null, BitmapFactory.Options())
      } catch (e: IOException) {
        log.error("Error loading image :", path, e)
      } finally {
        if (ins != null) {
          try {
            ins.close()
          } catch (e: IOException) {
          }
        }
      }
      return bitmap
    }

    init {
      try {
        val assetMgr = context.assets
        starfieldBitmap = loadBitmap(assetMgr, "stars/starfield.png")
        gasBitmap = loadBitmap(assetMgr, "stars/gas.png")
      } catch (e: Exception) {
        // Ignore.
      }
    }
  }
}