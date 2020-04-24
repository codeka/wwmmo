package au.com.codeka.warworlds.common

import java.util.*
import kotlin.math.cos

/**
 * This class generates perlin noise, which we can apply to various parts of the planet.
 */
open class PerlinNoise {
  protected var persistence = 0.0
  protected var interpolator: Interpolator = NoneInterpolator()
  protected var rawSeed: Long = 0
  protected var startOctave = 0
  protected var endOctave = 0
  protected val rawRand = Random()

  /**
   * Renders this [PerlinNoise] to the given [Image]. Useful mainly for testing/debugging.
   */
  fun render(img: Image) {
    for (y in 0 until img.height) {
      for (x in 0 until img.width) {
        val u = x.toDouble() / img.width.toDouble()
        val v = y.toDouble() / img.height.toDouble()
        val noise = getNoise(u, v)
        val c = Colour(1.0, noise, noise, noise)
        img.setPixelColour(x, y, c)
      }
    }
  }

  /**
   * Gets the noise value at the given (u,v) coordinate (which we assume range
   * from 0..1);
   */
  fun getNoise(u: Double, v: Double): Double {
    var total = 0.0
    for (octave in 0..endOctave - startOctave) {
      val freq = Math.pow(2.0, octave + startOctave.toDouble()) + 1
      val amplitude = Math.pow(persistence, octave.toDouble())
      val x = u * freq
      val y = v * freq
      val n = interpolatedNoise(x, y, octave)
      total += n * amplitude
    }
    total = total / 2.0 + 0.5
    if (total < 0.0) total = 0.0
    if (total > 1.0) total = 1.0
    return total
  }

  private fun rawNoise(x: Int, y: Int, octave: Int): Double {
    val seed = octave * 1000000L + x * 1000000000L + y * 100000000000L xor rawSeed
    rawRand.setSeed(seed)
    val r = rawRand.nextDouble()

    // we want the value to be between -1 and +1
    return r * 2.0 - 1.0
  }

  private fun interpolatedNoise(x: Double, y: Double, octave: Int): Double {
    val ix = x.toInt()
    val fx = x - ix.toDouble()
    val iy = y.toInt()
    val fy = y - iy.toDouble()
    val nx1y1 = rawNoise(ix, iy, octave)
    val nx2y1 = rawNoise(ix + 1, iy, octave)
    val nx1y2 = rawNoise(ix, iy + 1, octave)
    val nx2y2 = rawNoise(ix + 1, iy + 1, octave)
    val ny1 = interpolator.interpolate(nx1y1, nx2y1, fx)
    val ny2 = interpolator.interpolate(nx1y2, nx2y2, fx)
    return interpolator.interpolate(ny1, ny2, fy)
  }

  protected interface Interpolator {
    fun interpolate(a: Double, b: Double, n: Double): Double
  }

  protected class NoneInterpolator : Interpolator {
    override fun interpolate(a: Double, b: Double, n: Double): Double {
      return a
    }
  }

  protected class LinearInterpolator : Interpolator {
    override fun interpolate(a: Double, b: Double, n: Double): Double {
      return a + n * (b - a)
    }
  }

  protected class CosineInterpolator : Interpolator {
    override fun interpolate(a: Double, b: Double, n: Double): Double {
      var n = n
      val radians = n * Math.PI
      n = (1 - cos(radians)) * 0.5
      return a + n * (b - a)
    }
  }
}