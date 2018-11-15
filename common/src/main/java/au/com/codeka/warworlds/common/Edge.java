package au.com.codeka.warworlds.common;

import java.util.Collection;
import java.util.List;

/**
 * An edge is a two points from a list of points.
 */
public class Edge {
  public List<Vector2> points;
  public int a;
  public int b;

  public Edge(List<Vector2> points, int a, int b) {
    this.points = points;
    this.a = a;
    this.b = b;
  }

  public boolean isIn(Collection<Edge> edges) {
    for (Edge other : edges) {
      if (other == this) {
        // ignore ourselves in the collection
        continue;
      }

      if ((other.a == a && other.b == b) ||
          (other.b == a && other.a == b)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return a ^ b;
  }
}