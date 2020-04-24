package au.com.codeka.warworlds.common

/** A triangle is simply three vertices from a list of points. */
class Triangle(private val points: List<Vector2?>?, var a: Int, var b: Int, var c: Int) {

  // properties of the circumscribed circle of this triangle
  var centre: Vector2? = null
  var radius = 0.0
  fun hasVertex(index: Int): Boolean {
    return a == index || b == index || c == index
  }

  /**
   * Checks whether this triangle shared a vertex with the given other triangle.
   */
  fun shareVertex(t: Triangle): Boolean {
    return t.hasVertex(a) || t.hasVertex(b) || t.hasVertex(c)
  }

  /**
   * Finds an \c Edge of this triangle that shared the two given points, or \c null if
   * no edge of this triangle shares those points.
   */
  fun findEdge(p1: Vector2?, p2: Vector2?): Edge? {
    val av = points!![a]
    val bv = points[b]
    val cv = points[c]
    var i1 = -1
    var i2 = -1
    if (av!!.equals(p1, EPSILON) || av.equals(p2, EPSILON)) {
      if (i1 < 0) i1 = a else i2 = a
    }
    if (bv!!.equals(p1, EPSILON) || bv.equals(p2, EPSILON)) {
      if (i1 < 0) i1 = b else i2 = b
    }
    if (cv!!.equals(p1, EPSILON) || cv.equals(p2, EPSILON)) {
      if (i1 < 0) i1 = c else i2 = c
    }
    return if (i1 >= 0 && i2 >= 0) {
      Edge(points, i1, i2)
    } else {
      null
    }
  }

  fun findOppositeEdge(pt: Vector2?): Edge? {
    return if (pt!!.equals(points!![a], EPSILON)) {
      Edge(points, b, c)
    } else if (pt.equals(points[b], EPSILON)) {
      Edge(points, a, c)
    } else if (pt.equals(points[c], EPSILON)) {
      Edge(points, a, b)
    } else {
      null
    }
  }

  /**
   * Given \c pt (one of our vertices) we return the \c Edge that is on the
   * left hand side (that is, the other \c Edge contining this vertex will be
   * clockwise away from this one)
   */
  fun findCcwEdge(pt: Vector2?): Edge? {
    val p1: Int
    val p2: Int
    val p3: Int
    if (pt!!.equals(points!![a], 0.00000001)) {
      p1 = a
      p2 = b
      p3 = c
    } else if (pt.equals(points[b], 0.00000001)) {
      p1 = b
      p2 = c
      p3 = a
    } else if (pt.equals(points[c], 0.00000001)) {
      p1 = c
      p2 = b
      p3 = b
    } else {
      return null
    }

    // p1 is guaranteed to be pt by this point. We just need to work out
    // if p2 or p3 is the other side of the edge. We'll assume the other one
    // is p2 and check if that results in clockwise winding. If not then it
    // must be p3.
    // See: http://stackoverflow.com/a/1165943/241462
    val o1 = points[p2]
    val o2 = points[p3]
    var sum = (o1!!.x - pt.x) * (o1.y + pt.y)
    sum += (o2!!.x - o1.x) * (o2.y + o1.y)
    sum += (pt.x - o2.x) * (pt.y + o2.y)
    return if (sum >= 0) {
      Edge(points, p1, p2)
    } else {
      Edge(points, p1, p3)
    }
  }

  fun findCwEdge(pt: Vector2?): Edge? {
    val ccwEdge = findCcwEdge(pt)
    val oppositePt = findOppositePoint(ccwEdge)
    return findEdge(oppositePt, pt)
  }

  /**
   * Given an edge of this triangles, returns the \c Vector2 point that represents the
   * opposite point (i.e. the point that does \i not belong to this edge).
   */
  fun findOppositePoint(edge: Edge?): Vector2? {
    if (edge!!.a != a && edge.b != a) {
      return points!![a]
    }
    if (edge.a != b && edge.b != b) {
      return points!![b]
    }
    return if (edge.a != c && edge.b != c) {
      points!![c]
    } else null
  }

  /**
   * Calculates the centre and radius of the circumscribed circle around this triangle.
   */
  private fun calculateCircumscribedCircle(points: List<Vector2?>?) {
    val v1 = points!![a]
    val v2 = points[b]
    val v3 = points[c]
    if (Math.abs(v1!!.y - v2!!.y) < EPSILON && Math.abs(v2.y - v3!!.y) < EPSILON) {
      // the points are coincident, we can't do this...
      return
    }
    val xc: Double
    val yc: Double
    if (Math.abs(v2.y - v1.y) < EPSILON) {
      val m = -(v3!!.x - v2.x) / (v3.y - v2.y)
      val mx = (v2.x + v3.x) / 2.0
      val my = (v2.y + v3.y) / 2.0
      xc = (v2.x + v1.x) / 2.0
      yc = m * (xc - mx) + my
    } else if (Math.abs(v3!!.y - v2.y) < EPSILON) {
      val m = -(v2.x - v1.x) / (v2.y - v1.y)
      val mx = (v1.x + v2.x) / 2.0
      val my = (v1.y + v2.y) / 2.0
      xc = (v3.x + v2.x) / 2.0
      yc = m * (xc - mx) + my
    } else {
      val m1 = -(v2.x - v1.x) / (v2.y - v1.y)
      val m2 = -(v3.x - v2.x) / (v3.y - v2.y)
      val mx1 = (v1.x + v2.x) / 2.0
      val mx2 = (v2.x + v3.x) / 2.0
      val my1 = (v1.y + v2.y) / 2.0
      val my2 = (v2.y + v3.y) / 2.0
      xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2)
      yc = m1 * (xc - mx1) + my1
    }
    centre = Vector2(xc, yc)
    radius = centre!!.distanceTo(v2)
  }

  /**
   * Checks whether the given point is inside a circle that is defined by the three
   * points in this triangle.
   *
   *
   * That is, if we trace a circle such that the circle touches all three points of this
   * triangle, then this method will return \c true if the given point is inside that circle,
   * or \c false if it is not.
   */
  fun isInCircumscribedCircle(pt: Vector2?): Boolean {
    return if (centre == null) false else pt!!.distanceTo(centre) < radius
  }

  companion object {
    const val EPSILON = 0.000000001
  }

  init {
    calculateCircumscribedCircle(points)
  }
}