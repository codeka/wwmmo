package au.com.codeka.warworlds.common

/**
 * An edge is a two points from a list of points.
 */
class Edge(var points: List<Vector2?>?, var a: Int, var b: Int) {
  fun isIn(edges: Collection<Edge>): Boolean {
    for (other in edges) {
      if (other === this) {
        // ignore ourselves in the collection
        continue
      }
      if (other.a == a && other.b == b ||
          other.b == a && other.a == b) {
        return true
      }
    }
    return false
  }

  override fun hashCode(): Int {
    return a xor b
  }
}