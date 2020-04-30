package au.com.codeka.warworlds.common

import java.util.*

/**
 * This class represents a Voronoi diagram (and corresponding Delaunay triangulation) of a
 * [PointCloud]. It's used as the staring point of our texture generator, control field generator
 * and lots of other things.
 */
open class Voronoi(protected var pointCloud: PointCloud) {
  protected var triangles: ArrayList<Triangle>? = null

  // This mapping maps points in the point cloud to the triangles that share the point as a vertex.
  protected var pointCloudToTriangles: HashMap<Vector2, MutableList<Triangle>>? = null

  // Maps from points to a list of the neighbouring points.
  protected var pointNeighbours: HashMap<Vector2, List<Vector2>>? = null

  /**
   * Constructs a [Voronoi] diagram from the given [PointCloud]. You must call [.generate] to
   * actually generate the triangulation/voronoi.
   */
  init {
    generate()
  }

  /** Generates the delaunay triangulation/voronoi diagram of the point cloud. */
  protected fun generate() {
    var newTriangles: MutableList<Triangle>? = ArrayList()
    val points = pointCloud.points

    // First, create a "super triangle" that encompasses the whole point cloud. This is easy because
    // the point cloud is confined to the range (0,0)-(1,1) so we just add two triangles to
    // encompass that (plus a little bit of leeway).
    val superTriangles = createSuperTriangles(points, newTriangles)

    // Go through the vertices and add them.
    val size = points.size
    for (i in 0 until size) {
      // Add this vertex to the triangulation.
      addVertex(points, newTriangles, i)
    }

    // Now go through the triangles and copy any that don't share a vertex with the super triangles
    // to the final array.
    triangles = ArrayList()
    var numTriangles = newTriangles!!.size
    val numSuperTriangles = superTriangles.size
    for (i in 0 until numTriangles) {
      val t = newTriangles[i]
      var sharesVertex = false
      for (j in 0 until numSuperTriangles) {
        val st = superTriangles[j]
        if (st.shareVertex(t)) {
          sharesVertex = true
          break
        }
      }
      if (!sharesVertex) {
        triangles!!.add(t)
      }
    }

    // Remove the super triangle points (they'll be the last four we added).
    for (i in 0..3) {
      pointCloud.points.removeAt(pointCloud.points.size - 1)
    }

    // Next, go through the list of triangles and populate pointCloudToTriangles.
    pointCloudToTriangles = HashMap()
    numTriangles = triangles!!.size
    for (i in 0 until numTriangles) {
      val t = triangles!![i]
      addTriangleToPointCloudToTrianglesMap(points[t.a], t)
      addTriangleToPointCloudToTrianglesMap(points[t.b], t)
      addTriangleToPointCloudToTrianglesMap(points[t.c], t)
    }
    sortPointCloudToTrianglesMap()

    // Finally, go through the points again and work out all of that point's neighbours.
    pointNeighbours = HashMap()
    val numPoints = points.size
    for (i in 0 until numPoints) {
      val pt = points[i]
      newTriangles = pointCloudToTriangles!![pt]
      if (newTriangles != null) {
        val neighbours: MutableSet<Vector2> = HashSet()
        numTriangles = newTriangles.size
        for (j in 0 until numTriangles) {
          val t = newTriangles[j]
          val edge = t.findOppositeEdge(pt)
          neighbours.add(points[edge!!.a])
          neighbours.add(points[edge.b])
        }
        pointNeighbours!![pt] = ArrayList(neighbours)
      }
    }
  }

  /** Finds the point closest to the given input point. */
  fun findClosestPoint(uv: Vector2): Vector2? {
    var closestPoint: Vector2? = null
    var closestDistance2 = 0.0
    val points = pointCloud.points
    val numPoints = points.size
    for (i in 0 until numPoints) {
      val pt = points[i]
      if (closestPoint == null) {
        closestPoint = pt
        closestDistance2 = pt.distanceTo2(uv)
      } else {
        val distance2 = pt.distanceTo2(uv)
        if (distance2 < closestDistance2) {
          closestDistance2 = distance2
          closestPoint = pt
        }
      }
    }
    return closestPoint
  }

  /** Gets the points that neighbour the given point. */
  fun getNeighbours(pt: Vector2): List<Vector2>? {
    return pointNeighbours!![pt]
  }

  /**
   * When we first add triangles to the \c pointCloudToTriangles map, they're just added in any old
   * order. This doesn't work when trying to determine if a point is inside the cell or drawing the
   * outline etc, so we need to make sure the triangles are added in clockwise order around each
   * point. This method does that.
   */
  private fun sortPointCloudToTrianglesMap() {
    for (pt in pointCloud.points) {
      val unsorted = pointCloudToTriangles!![pt] ?: continue
      val sorted = ArrayList<Triangle>()
      sorted.add(unsorted[0])
      unsorted.removeAt(0)
      var nextEdge = sorted[0].findCcwEdge(pt)
      while (!unsorted.isEmpty()) {
        val t = sorted[sorted.size - 1]
        val otherPt = t.findOppositePoint(nextEdge)
        for (nextTriangle in unsorted) {
          nextEdge = nextTriangle.findEdge(pt, otherPt)
          if (nextEdge != null) {
            unsorted.remove(nextTriangle)
            sorted.add(nextTriangle)
            break
          }
        }
        if (nextEdge == null) {
          // If there's no more edges in the clockwise direction then it means we're at the edge of
          // the diagram. We'll break out of this loop and start working anti-clockwise the other
          // way.
          break
        }
      }
      if (unsorted.isNotEmpty()) {
        // If we come in here, then means we got to the edge of the world and we now have to start
        // working anti-clockwise.
        nextEdge = sorted[0].findCwEdge(pt)
        while (unsorted.isNotEmpty()) {
          if (nextEdge == null) {
            break // TODO: this is an error...
          }
          val t = sorted[0]
          val otherPt = t.findOppositePoint(nextEdge)
          for (nextTriangle in unsorted) {
            nextEdge = nextTriangle.findEdge(pt, otherPt)
            if (nextEdge != null) {
              unsorted.remove(nextTriangle)
              sorted.add(0, nextTriangle)
              break
            }
          }
        }
      }
      pointCloudToTriangles!![pt] = sorted
    }
  }

  /**
   * Helper class to render the delaunay triangulation to the given [Image]. We draw lines of the
   * given [Colour].
   */
  fun renderDelaunay(img: Image, c: Colour) {
    val points = pointCloud.points
    for (t in triangles!!) {
      var p1 = points[t.a]
      var p2 = points[t.b]
      drawLine(img, c, p1, p2)
      p1 = points[t.c]
      drawLine(img, c, p1, p2)
      p2 = points[t.a]
      drawLine(img, c, p1, p2)
    }
  }

  /**
   * Helper class to render the Voronoi diagram to the given [Image]. We draw lines of the given
   * [Colour].
   */
  fun renderVoronoi(img: Image, c: Colour) {
    for (pt in pointCloud.points) {
      val triangles = pointCloudToTriangles!![pt]
          ?: // shouldn't happen, but just in case...
          continue
      for (i in 0 until triangles.size - 1) {
        drawLine(img, c, triangles[i].centre, triangles[i + 1].centre)
      }
      drawLine(img, c, triangles[0].centre, triangles[triangles.size - 1].centre)
    }
  }

  /**
   * Gets a list of triangles that share the given point as a vertex.
   */
  fun getTrianglesForPoint(pt: Vector2?): List<Triangle> {
    return pointCloudToTriangles!![pt]!!
  }

  private fun drawLine(img: Image, c: Colour, p1: Vector2?, p2: Vector2?) {
    val x1 = (img.width * p1!!.x).toInt()
    val x2 = (img.width * p2!!.x).toInt()
    val y1 = (img.height * p1.y).toInt()
    val y2 = (img.height * p2.y).toInt()
    img.drawLine(x1, y1, x2, y2, c)
  }

  private fun addTriangleToPointCloudToTrianglesMap(pt: Vector2, t: Triangle) {
    var value = pointCloudToTriangles!![pt]
    if (value == null) {
      value = ArrayList()
      pointCloudToTriangles!![pt] = value
    }
    value.add(t)
  }

  /**
   * Adds a new point vertex (given by \c vIndex) to the given triangulation.
   */
  private fun addVertex(points: List<Vector2?>?, triangles: MutableList<Triangle>?, vIndex: Int) {
    val pt = points!![vIndex]

    // extract all triangles in the circumscribed circle around this point
    val circumscribedTriangles = ArrayList<Triangle>()
    for (t in triangles!!) {
      if (t.isInCircumscribedCircle(pt)) {
        circumscribedTriangles.add(t)
      }
    }
    triangles.removeAll(circumscribedTriangles)

    // create an edge buffer of all edges in those triangles
    val edgeBuffer = ArrayList<Edge>()
    for (t in circumscribedTriangles) {
      edgeBuffer.add(Edge(points, t.a, t.b))
      edgeBuffer.add(Edge(points, t.b, t.c))
      edgeBuffer.add(Edge(points, t.c, t.a))
    }

    // extract all of the edges that aren't doubled-up
    val uniqueEdges = ArrayList<Edge>()
    for (e in edgeBuffer) {
      if (!e.isIn(edgeBuffer)) {
        uniqueEdges.add(e)
      }
    }

    // now add triangles using the unique edges and the new point!
    for (e in uniqueEdges) {
      triangles.add(Triangle(points, vIndex, e.a, e.b))
    }
  }

  /**
   * Creates two "super" triangles that encompasses all points in the point cloud.
   *
   * @param points    The point cloud points.
   * @param triangles The triangle list we'll be generating.
   * @return The super triangles we added. Any triangles in the final triangulation that share
   * a vertex with these will need to be removed as well.
   */
  private fun createSuperTriangles(
      points: MutableList<Vector2>, triangles: MutableList<Triangle>?): List<Triangle> {
    var minX = 1.0
    var minY = 1.0
    var maxX = 0.0
    var maxY = 0.0
    for (pt in points) {
      if (pt.x < minX) {
        minX = pt.x
      }
      if (pt.x > maxX) {
        maxX = pt.x
      }
      if (pt.y < minY) {
        minY = pt.y
      }
      if (pt.y > maxY) {
        maxY = pt.y
      }
    }
    points.add(Vector2(minX - 0.1, minY - 0.1))
    points.add(Vector2(minX - 0.1, maxY + 0.1))
    points.add(Vector2(maxX + 0.1, maxY + 0.1))
    points.add(Vector2(maxX + 0.1, minY - 0.1))
    val superTriangles = ArrayList<Triangle>()
    superTriangles.add(Triangle(points, points.size - 4, points.size - 3, points.size - 2))
    superTriangles.add(Triangle(points, points.size - 4, points.size - 2, points.size - 1))
    for (t in superTriangles) {
      triangles!!.add(t)
    }
    return superTriangles
  }
}