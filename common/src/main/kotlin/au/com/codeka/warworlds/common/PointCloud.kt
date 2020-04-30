package au.com.codeka.warworlds.common

import java.util.*

/**
 * A point cloud is, well, a cloud of points. We use it to generate a voronoi/delaunay mapping
 * that is then use to generate planet textures.
 *
 * The points are always bound to the square (0,0), (1,1).
 */
open class PointCloud {
  protected var points_: ArrayList<Vector2>

  constructor(points: ArrayList<Vector2> = ArrayList()) {
    points_ = points
  }

  val points: MutableList<Vector2>
    get() = points_

  /**
   * Helper class to render this point cloud to the given \c Image (mostly for debugging).
   */
  fun render(img: Image) {
    for (p in points_) {
      val x = (img.width * p.x).toInt()
      val y = (img.height * p.y).toInt()
      img.drawCircle(x, y, 5.0, Colour.RED)
    }
  }

  /**
   * Generates points by simply generating random (x,y) coordinates. This isn't usually very useful,
   * because the points tend to clump up and look unrealistic.
   */
  open class RandomGenerator {
    fun generate(density: Double, rand: Random): ArrayList<Vector2> {
      // numPointsFactor will be a number between 0.75 and 1.25 which we'll use to adjust the number
      // of points we generate
      var numPointsFactor = rand.nextDouble()
      numPointsFactor = 0.75 + 0.5 * numPointsFactor
      var numPoints = 25 + (475 * density * numPointsFactor).toInt()
      if (numPoints < 25) {
        numPoints = 25
      }
      val points = ArrayList<Vector2>(numPoints)
      for (i in 0 until numPoints) {
        points.add(Vector2(rand.nextDouble(), rand.nextDouble()))
      }
      return points
    }
  }

  /**
   * Uses a poisson generator to generate more "natural" looking random points than the basic
   * [RandomGenerator] does.
   */
  open class PoissonGenerator {
    fun generate(density: Double, randomness: Double, rand: Random): ArrayList<Vector2> {
      val points = ArrayList<Vector2>(30) // give us some initial capacity
      val unprocessed = ArrayList<Vector2>(50) // give us some initial capacity
      unprocessed.add(Vector2(rand.nextDouble(), rand.nextDouble()))

      // we want minDistance to be small when density is high and big when density is small.
      val minDistance = 0.001 + 1.0 / density * 0.03

      // packing is how many points around each point we'll test for a new location.
      // a high randomness means we'll have a low number (more random) and a low randomness
      // means a high number (more uniform).
      val packing = 10 + ((1.0 - randomness) * 90).toInt()
      while (!unprocessed.isEmpty()) {
        // get a random point from the unprocessed list
        val index = rand.nextInt(unprocessed.size)
        val point = unprocessed[index]
        unprocessed.removeAt(index)

        // if there's another point too close to this one, ignore it
        if (inNeighbourhood(points, point, minDistance)) {
          continue
        }

        // otherwise, this is a good one
        points.add(point)

        // now generate a bunch of points around this one...
        for (i in 0 until packing) {
          val newPoint = generatePointAround(rand, point, minDistance)
          if (newPoint.x < 0.0 || newPoint.x > 1.0) {
            continue
          }
          if (newPoint.y < 0.0 || newPoint.y > 1.0) {
            continue
          }
          unprocessed.add(newPoint)
        }
      }
      return points
    }

    /**
     * Generates a new point around the given centre point and at least \c minDistance from it.
     */
    private fun generatePointAround(rand: Random, point: Vector2, minDistance: Double): Vector2 {
      val radius = minDistance * (1.0 + rand.nextDouble())
      val angle = 2.0 * Math.PI * rand.nextDouble()
      return Vector2(
          point.x + radius * Math.cos(angle),
          point.y + radius * Math.sin(angle))
    }

    /**
     * Checks whether the given new point is too close to any previously-generated points.
     *
     * @param points      The list of points we've already generated.
     * @param point       The point we've just generated and need to check.
     * @param minDistance The minimum distance we accept as being valid.
     * @return Whether the given point is within the neighbourhood of the existing points.
     */
    private fun inNeighbourhood(
        points: List<Vector2>, point: Vector2, minDistance: Double): Boolean {
      val n = points.size
      for (i in 0 until n) {
        val otherPoint = points[i]
        if (point.distanceTo(otherPoint) < minDistance) {
          return true
        }
      }
      return false
    }
  }
}