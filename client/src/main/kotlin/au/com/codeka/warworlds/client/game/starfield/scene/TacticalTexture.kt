package au.com.codeka.warworlds.client.game.starfield.scene

import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.opengl.Texture
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.SectorCoord
import au.com.codeka.warworlds.common.proto.Star
import com.google.common.base.Preconditions
import java.util.*

/**
 * A [Texture] that returns a bitmap to use as a texture for the "tactical" SceneObject that
 * covers a sector.
 */
class TacticalTexture private constructor(private val sectorCoord: SectorCoord) : Texture() {
  private var bitmap: Bitmap? = null

  init {
    App.taskRunner.runTask({ bitmap = createBitmap(sectorCoord) }, Threads.BACKGROUND)
  }

  override fun bind() {
    if (bitmap != null) {
      setTextureId(createGlTexture())
      bitmap?.recycle()
      bitmap = null
    }
    super.bind()
  }

  private fun createGlTexture(): Int {
    Preconditions.checkState(bitmap != null)
    val textureHandleBuffer = IntArray(1)
    GLES20.glGenTextures(1, textureHandleBuffer, 0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandleBuffer[0])
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    return textureHandleBuffer[0]
  }

  companion object {
    private val log = Log("TacticalTexture")

    // The size of the texture, in pixels, that we'll generate.
    private const val TEXTURE_SIZE = 128

    // The radius of the circle, in pixels, that we'll put around each empire.
    private const val CIRCLE_RADIUS = 30

    private val gradientColors = intArrayOf(0, 0)
    private val gradientStops = floatArrayOf(0.1f, 1.0f)

    private val textureCache = TacticalTextureLruCache(16)

    fun create(sectorCoord: SectorCoord): TacticalTexture {
      synchronized(textureCache) {
        val cachedTexture = textureCache[sectorCoord]
        if (cachedTexture != null) {
          return cachedTexture
        }
        val texture = TacticalTexture(sectorCoord)
        textureCache.put(sectorCoord, texture)
        return texture
      }
    }

    private fun createBitmap(sectorCoord: SectorCoord): Bitmap {
      log.info("Generating bitmap for ${sectorCoord.x},${sectorCoord.y}...")
      val startTime = System.currentTimeMillis()

      val bmp = Bitmap.createBitmap(TEXTURE_SIZE, TEXTURE_SIZE, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val paint = Paint()
      paint.style = Paint.Style.FILL

      // We have to look at sectors around this one as well, so that edges look right
      for (offsetY in -1..1) {
        for (offsetX in -1..1) {
          drawCircles(sectorCoord, offsetX, offsetY, canvas, paint)
        }
      }

      val endTime = System.currentTimeMillis()
      log.info("Bitmap for ${sectorCoord.x},${sectorCoord.y} generated in ${endTime - startTime}ms")
      return bmp
    }

    private fun drawCircles(
        sectorCoord: SectorCoord, offsetX: Int, offsetY: Int, canvas: Canvas, paint: Paint) {
      val scaleFactor = TEXTURE_SIZE.toFloat() / 1024f
      StarManager.searchSectorStars(
          SectorCoord(x = sectorCoord.x + offsetX, y = sectorCoord.y + offsetY)).use {
        starCursor ->
          for (star in starCursor) {
            val x = (star.offset_x + offsetX * 1024.0f) * scaleFactor
            val y = (star.offset_y + offsetY * 1024.0f) * scaleFactor
            val radius = CIRCLE_RADIUS.toFloat()
            if (x < -radius || x > TEXTURE_SIZE + radius ||
                y < -radius || y > TEXTURE_SIZE + radius) {
              // if it's completely off the bitmap, skip drawing it
              continue
            }
            var color = 0
            var empireId = getEmpireId(star)
            if (empireId == null) {
              empireId = getFleetOnlyEmpire(star)
              if (empireId != null) {
                color = getShieldColour(empireId)
                color = 0x66ffffff and color
              }
            } else {
              color = getShieldColour(empireId)
            }

            gradientColors[0] = color
            gradientColors[1] = color and 0x00ffffff
            val gradient = RadialGradient(
                x, y, CIRCLE_RADIUS.toFloat(), gradientColors, gradientStops, Shader.TileMode.CLAMP)
            paint.shader = gradient
            canvas.drawCircle(x, y, radius, paint)
          }
        }
    }

    // TODO: this logic is basically cut'n'paste from EmpireRendererHandler on the server.
    private fun getShieldColour(empireId: Long?): Int {
      if (empireId == null) {
        return 0
      }
      val rand = Random(empireId xor 7438274364563846L)
      return -0x1000000 or (
          rand.nextInt(100) + 100 shl 16) or (
          rand.nextInt(100) + 100 shl 8) or
          rand.nextInt(100) + 100
    }

    private fun getEmpireId(star: Star): Long? {
      // if it's a wormhole, the empire is the owner of the wormhole
//    BaseStar.WormholeExtra wormholeExtra = star.getWormholeExtra();
//    if (wormholeExtra != null) {
//      return EmpireManager.i.getEmpire(wormholeExtra.getEmpireID());
//    }

      // otherwise, pick the first colony we find to represent the star
      // TODO: what if the star is colonized by more than one empire?
      for (planet in star.planets) {
        val colony = planet.colony
        if (colony?.empire_id != null) {
          return colony.empire_id
        }
      }

      return null
    }

    /**
     * If we can't find an empire by colony, look for a fleet. If there's one, we'll display a circle
     * that a little dimmed.
     */
    private fun getFleetOnlyEmpire(star: Star): Long? {
      // TODO: what if there is more than one fleet? Should try to mix the colors?
      for (fleet in star.fleets) {
        if (fleet.empire_id != null) {
          return fleet.empire_id
        }
      }
      return null
    }
  }
}