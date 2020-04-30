package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.PointCloud
import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.planetrender.Template.PointCloudTemplate
import java.util.*

/** A [PointCloud] that takes it's parameters from a [Template].  */
class TemplatedPointCloud(tmpl: PointCloudTemplate, rand: Random) : PointCloud(ArrayList()) {
  /**
   * This is the base class for implementations that generate point clouds. We contain the
   * various properties the control how many points to generate, "randomness" etc.
   */
  interface Generator {
    fun generate(tmpl: PointCloudTemplate, rand: Random): ArrayList<Vector2>
  }

  /**
   * Generates points by simply generating random (x,y) coordinates. This isn't usually
   * very useful, because the points tend to clump up and look unrealistic.
   */
  class TemplatedRandomGenerator : RandomGenerator(), Generator {
    override fun generate(tmpl: PointCloudTemplate, rand: Random): ArrayList<Vector2> {
      return generate(tmpl.density, rand)
    }
  }

  /**
   * Uses a poisson generator to generate more "natural" looking random points than the
   * basic [RandomGenerator] does.
   */
  class TemplatedPoissonGenerator : PoissonGenerator(), Generator {
    override fun generate(tmpl: PointCloudTemplate, rand: Random): ArrayList<Vector2> {
      return generate(tmpl.density, tmpl.randomness, rand)
    }
  }

  init {
    val g = when (tmpl.generator) {
      PointCloudTemplate.Generator.Random -> {
        TemplatedRandomGenerator()
      }
      PointCloudTemplate.Generator.Poisson -> {
        TemplatedPoissonGenerator()
      }
      else -> {
        throw RuntimeException("Unknown PointCloudGenerator: " + tmpl.generator)
      }
    }
    points_ = g.generate(tmpl, rand)
  }
}