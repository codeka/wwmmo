package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.PerlinNoise
import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.common.Vector3
import au.com.codeka.warworlds.planetrender.Template.PerlinNoiseTemplate
import au.com.codeka.warworlds.planetrender.Template.WarpTemplate
import java.util.*

/**
 * This class takes a ray that's going in a certain direction and warps it based on a noise pattern.
 * This is used to generate misshapen asteroid images, for example.
 */
class RayWarper(tmpl: WarpTemplate, rand: Random) {
  private var noiseGenerator: NoiseGenerator? = null
  private val warpFactor: Double
  fun warp(vec: Vector3, u: Double, v: Double) {
    noiseGenerator!!.warp(vec, u, v, warpFactor)
  }

  internal abstract class NoiseGenerator {
    protected open fun getNoise(u: Double, v: Double): Double {
      return 0.0
    }

    protected fun getValue(u: Double, v: Double): Vector3 {
      val x = getNoise(u * 0.25, v * 0.25)
      val y = getNoise(0.25 + u * 0.25, v * 0.25)
      val z = getNoise(u * 0.25, 0.25 + v * 0.25)
      return Vector3(x, y, z)
    }

    open fun warp(vec: Vector3, u: Double, v: Double, factor: Double) {
      val warpVector = getValue(u, v)
      warpVector.reset(warpVector.x * factor + (1.0 - factor),
          warpVector.y * factor + (1.0 - factor),
          warpVector.z * factor + (1.0 - factor))
      vec.reset(vec.x * warpVector.x,
          vec.y * warpVector.y,
          vec.z * warpVector.z)
    }
  }

  internal class PerlinGenerator(tmpl: WarpTemplate, rand: Random) : NoiseGenerator() {
    private val noise: PerlinNoise
    public override fun getNoise(u: Double, v: Double): Double {
      return noise.getNoise(u, v)
    }

    init {
      noise = TemplatedPerlinNoise(tmpl.getParameter(PerlinNoiseTemplate::class.java)!!, rand)
    }
  }

  internal class SpiralGenerator : NoiseGenerator() {
    override fun warp(vec: Vector3, u: Double, v: Double, factor: Double) {
      val uv = Vector2(u, v)
      uv.rotate(factor * uv.length() * 2.0 * Math.PI * 2.0 / 360.0)
      vec.reset(uv.x, -uv.y, 1.0)
    }
  }

  init {
    if (tmpl.noiseGenerator == WarpTemplate.NoiseGenerator.Perlin) {
      noiseGenerator = PerlinGenerator(tmpl, rand)
    } else if (tmpl.noiseGenerator == WarpTemplate.NoiseGenerator.Spiral) {
      noiseGenerator = SpiralGenerator()
    }
    warpFactor = tmpl.warpFactor
  }
}