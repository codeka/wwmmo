package au.com.codeka.warworlds.server.util

import com.google.common.base.Objects

/**
 * Helper class that represents a pair of values.
 */
class Pair<E, F>(one: E, two: F) {
  var one: E? = one
  var two: F? = two

  override fun hashCode(): Int {
    return Objects.hashCode(one, two)
  }

  override fun equals(obj: Any?): Boolean {
    if (obj == null || obj !is Pair<*, *>) {
      return false
    }
    val other: Pair<E, F> = obj as Pair<E, F>
    return Objects.equal(other.one, one) && Objects.equal(other.two, two)
  }
}