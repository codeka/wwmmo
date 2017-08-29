package au.com.codeka.warworlds.common;

import java.util.List;

/**
 * A triangle is simply three vertices from a list of points.
 */
public class Triangle {
  private List<Vector2> points;
  public int a;
  public int b;
  public int c;

  final static double EPSILON = 0.000000001;

  // properties of the circumscribed circle of this triangle
  public Vector2 centre;
  public double radius;

  public Triangle(List<Vector2> points, int a, int b, int c) {
    this.points = points;
    this.a = a;
    this.b = b;
    this.c = c;

    calculateCircumscribedCircle(points);
  }

  public boolean hasVertex(int index) {
    return (a == index || b == index || c == index);
  }

  /**
   * Checks whether this triangle shared a vertex with the given other triangle.
   */
  public boolean shareVertex(Triangle t) {
    return (t.hasVertex(a) || t.hasVertex(b) || t.hasVertex(c));
  }

  /**
   * Finds an \c Edge of this triangle that shared the two given points, or \c null if
   * no edge of this triangle shares those points.
   */
  public Edge findEdge(Vector2 p1, Vector2 p2) {
    Vector2 av = points.get(a);
    Vector2 bv = points.get(b);
    Vector2 cv = points.get(c);

    int i1 = -1;
    int i2 = -1;

    if (av.equals(p1, EPSILON) || av.equals(p2, EPSILON)) {
      if (i1 < 0)
        i1 = a;
      else
        i2 = a;
    }
    if (bv.equals(p1, EPSILON) || bv.equals(p2, EPSILON)) {
      if (i1 < 0)
        i1 = b;
      else
        i2 = b;
    }
    if (cv.equals(p1, EPSILON) || cv.equals(p2, EPSILON)) {
      if (i1 < 0)
        i1 = c;
      else
        i2 = c;
    }

    if (i1 >= 0 && i2 >= 0) {
      return new Edge(points, i1, i2);
    } else {
      return null;
    }
  }

  public Edge findOppositeEdge(Vector2 pt) {
    if (pt.equals(points.get(a), EPSILON)) {
      return new Edge(points, b, c);
    } else if (pt.equals(points.get(b), EPSILON)) {
      return new Edge(points, a, c);
    } else if (pt.equals(points.get(c), EPSILON)) {
      return new Edge(points, a, b);
    } else {
      return null;
    }
  }

  /**
   * Given \c pt (one of our vertices) we return the \c Edge that is on the
   * left hand side (that is, the other \c Edge contining this vertex will be
   * clockwise away from this one)
   */
  public Edge findCcwEdge(Vector2 pt) {
    int p1, p2, p3;
    if (pt.equals(points.get(a), 0.00000001)) {
      p1 = a;
      p2 = b;
      p3 = c;
    } else if (pt.equals(points.get(b), 0.00000001)) {
      p1 = b;
      p2 = c;
      p3 = a;
    } else if (pt.equals(points.get(c), 0.00000001)) {
      p1 = c;
      p2 = b;
      p3 = b;
    } else {
      return null;
    }

    // p1 is guaranteed to be pt by this point. We just need to work out
    // if p2 or p3 is the other side of the edge. We'll assume the other one
    // is p2 and check if that results in clockwise winding. If not then it
    // must be p3.
    // See: http://stackoverflow.com/a/1165943/241462
    Vector2 o1 = points.get(p2);
    Vector2 o2 = points.get(p3);

    double sum = (o1.x - pt.x) * (o1.y + pt.y);
    sum += (o2.x - o1.x) * (o2.y + o1.y);
    sum += (pt.x - o2.x) * (pt.y + o2.y);

    if (sum >= 0) {
      return new Edge(points, p1, p2);
    } else {
      return new Edge(points, p1, p3);
    }
  }

  public Edge findCwEdge(Vector2 pt) {
    Edge ccwEdge = findCcwEdge(pt);
    Vector2 oppositePt = this.findOppositePoint(ccwEdge);
    return findEdge(oppositePt, pt);
  }

  /**
   * Given an edge of this triangles, returns the \c Vector2 point that represents the
   * opposite point (i.e. the point that does \i not belong to this edge).
   */
  public Vector2 findOppositePoint(Edge edge) {
    if (edge.a != a && edge.b != a) {
      return points.get(a);
    }
    if (edge.a != b && edge.b != b) {
      return points.get(b);
    }
    if (edge.a != c && edge.b != c) {
      return points.get(c);
    }

    return null;
  }

  /**
   * Calculates the centre and radius of the circumscribed circle around this triangle.
   */
  private void calculateCircumscribedCircle(List<Vector2> points) {
    final Vector2 v1 = points.get(a);
    final Vector2 v2 = points.get(b);
    final Vector2 v3 = points.get(c);

    if (Math.abs(v1.y - v2.y) < EPSILON && Math.abs(v2.y - v3.y) < EPSILON) {
      // the points are coincident, we can't do this...
      return;
    }

    double xc, yc;
    if (Math.abs(v2.y - v1.y) < EPSILON) {
      final double m = -(v3.x - v2.x) / (v3.y - v2.y);
      final double mx = (v2.x + v3.x) / 2.0;
      final double my = (v2.y + v3.y) / 2.0;
      xc = (v2.x + v1.x) / 2.0;
      yc = m * (xc - mx) + my;
    } else if (Math.abs(v3.y - v2.y) < EPSILON) {
      final double m = -(v2.x - v1.x) / (v2.y - v1.y);
      final double mx = (v1.x + v2.x) / 2.0;
      final double my = (v1.y + v2.y) / 2.0;
      xc = (v3.x + v2.x) / 2.0;
      yc = m * (xc - mx) + my;
    } else {
      final double m1 = -(v2.x - v1.x) / (v2.y - v1.y);
      final double m2 = -(v3.x - v2.x) / (v3.y - v2.y);
      final double mx1 = (v1.x + v2.x) / 2.0;
      final double mx2 = (v2.x + v3.x) / 2.0;
      final double my1 = (v1.y + v2.y) / 2.0;
      final double my2 = (v2.y + v3.y) / 2.0;
      xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
      yc = m1 * (xc - mx1) + my1;
    }

    centre = new Vector2(xc, yc);
    radius = centre.distanceTo(v2);
  }

  /**
   * Checks whether the given point is inside a circle that is defined by the three
   * points in this triangle.
   * <p/>
   * That is, if we trace a circle such that the circle touches all three points of this
   * triangle, then this method will return \c true if the given point is inside that circle,
   * or \c false if it is not.
   */
  public boolean isInCircumscribedCircle(Vector2 pt) {
    if (centre == null)
      return false;

    return pt.distanceTo(centre) < radius;
  }
}
