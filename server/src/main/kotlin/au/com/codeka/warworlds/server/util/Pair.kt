package au.com.codeka.warworlds.server.util

import com.google.common.base.Objects

/** Helper class that represents a pair of values. */
class Pair<E, F>(var one: E, var two: F) {
  override fun hashCode(): Int {
    return Objects.hashCode(one, two)
  }

  override fun equals(other: Any?): Boolean {
    if (other == null || other !is Pair<*, *>) {
      return false
    }
    return Objects.equal(other.one, one) && Objects.equal(other.two, two)
  }
}
