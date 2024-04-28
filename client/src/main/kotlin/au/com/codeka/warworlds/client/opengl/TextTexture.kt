package au.com.codeka.warworlds.client.opengl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLUtils
import au.com.codeka.warworlds.common.Log
import java.util.*
import kotlin.math.ceil

/**
 * This is a [Texture] which is used to back an image for drawing arbitrary strings.
 */
class TextTexture : Texture() {
  private val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  private val canvas: Canvas = Canvas(bitmap)
  private val paint: Paint = Paint()
  private val characters: MutableMap<Char, Rect> = HashMap()
  private var id: Int
  private var dirty = false

  /** The offset from the left of the texture that we'll draw the next character.  */
  private var currRowOffsetX = 0

  /** The offset from the top of the texture that we'll draw the next character.  */
  private var currRowOffsetY = 0
  override fun bind() {
    if (dirty) {
      if (id == -1) {
        val textureHandleBuffer = IntArray(1)
        GLES20.glGenTextures(1, textureHandleBuffer, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandleBuffer[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        id = textureHandleBuffer[0]
        setTextureId(id)
      }
      super.bind()
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
      dirty = false
      return
    }
    super.bind()
  }

  val textHeight: Float
    get() = TEXT_HEIGHT.toFloat()

  /** Gets the bounds of the given char.  */
  fun getCharBounds(ch: Char): Rect {
    var bounds = characters[ch]
    if (bounds == null) {
      ensureChar(ch)
      bounds = characters[ch]
    }
    return bounds!!
  }

  /** Ensures that we have cached all the characters needed to draw every character in the string. */
  private fun ensureText(str: String) {
    for (element in str) {
      ensureChar(element)
    }
  }

  private fun ensureChar(ch: Char) {
    if (characters.containsKey(ch)) {
      return
    }
    val str = String(charArrayOf(ch))
    val charWidth = ceil(paint.measureText(str).toDouble()).toInt()
    if (currRowOffsetX + charWidth > width) {
      currRowOffsetX = 0
      currRowOffsetY += ROW_HEIGHT
    }
    canvas.drawText(str,
        currRowOffsetX.toFloat(),
        currRowOffsetY + TEXT_HEIGHT - paint.descent() + (ROW_HEIGHT - TEXT_HEIGHT),
        paint)
    val bounds = Rect(currRowOffsetX, currRowOffsetY,
        currRowOffsetX + charWidth, currRowOffsetY + ROW_HEIGHT)
    characters[ch] = bounds
    currRowOffsetX += charWidth
    dirty = true
  }

  companion object {
    private val log = Log("TextTexture")

    // TODO: make the bitmap automatically resize if it needs to.
    const val width = 512
    const val height = 512

    /** The height of the text.  */
    private const val TEXT_HEIGHT = 28
    private const val ROW_HEIGHT = 32
  }

  init {
    paint.textSize = TEXT_HEIGHT.toFloat()
    paint.isAntiAlias = true
    paint.setARGB(255, 255, 255, 255)
    id = -1
    ensureText("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
        + "!@#$%^&*()`~-=_+[]\\{}|;':\",./<>? ")
  }
}
