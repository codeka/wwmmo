package au.com.codeka.warworlds.client.game.starfield

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.opengl.Texture
import au.com.codeka.warworlds.common.proto.Sector
import au.com.codeka.warworlds.common.proto.Star
import com.google.common.base.Preconditions
import java.util.*

/**
 * A [Texture] that returns a bitmap to use as a texture for the "tactical" SceneObject that
 * covers a sector.
 */
class TacticalTexture private constructor(sector: Sector) : Texture() {
  private var bitmap: Bitmap? = null
  override fun bind() {
    if (bitmap != null) {
      setTextureId(createGlTexture())
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
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    return textureHandleBuffer[0]
  }

  companion object {
    // The size of the texture, in pixels, that we'll generate.
    private const val TEXTURE_SIZE = 128

    // The radius of the circle, in pixels, that we'll put around each empire.
    private const val CIRCLE_RADIUS = 20
    fun create(sector: Sector): TacticalTexture {
      return TacticalTexture(sector)
    }

    private fun createBitmap(sector: Sector): Bitmap {
      val bmp = Bitmap.createBitmap(TEXTURE_SIZE, TEXTURE_SIZE, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val paint = Paint()
      paint.style = Paint.Style.FILL

      // We have to look at sectors around this one as well, so that edges look right
      for (offsetY in -1..1) {
        for (offsetX in -1..1) {
          var s: Sector?
          s = if (offsetX == 0 && offsetY == 0) {
            sector
          } else {
            null // TODO SectorManager.i.getSector(sector.getX() + offsetX, sector.getY() + offsetY);
          }
          if (s == null) {
            continue
          }
          drawCircles(s, offsetX, offsetY, canvas, paint)
        }
      }
      return bmp
    }

    private fun drawCircles(sector: Sector, offsetX: Int, offsetY: Int, canvas: Canvas, paint: Paint) {
      val scaleFactor = TEXTURE_SIZE.toFloat() / 1024f
      for (star in sector.stars) {
        val x = (star.offset_x + offsetX * 1024.0f) * scaleFactor
        val y = (star.offset_y + offsetY * 1024.0f) * scaleFactor
        val radius = CIRCLE_RADIUS.toFloat()
        if (x < -radius || x > TEXTURE_SIZE + radius || y < -radius || y > TEXTURE_SIZE + radius) {
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
        paint.color = color
        canvas.drawCircle(x, y, radius, paint)
      }
    }

    // TODO: this logic is basicaly cut'n'paste from EmpireRendererHandler on the server.
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
        if (planet.colony != null) {
          return planet.colony.empire_id
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
        return fleet.empire_id
      }
      return null
    }
  }

  init {
    App.i.taskRunner.runTask(Runnable { bitmap = createBitmap(sector) }, Threads.BACKGROUND)
  }
}