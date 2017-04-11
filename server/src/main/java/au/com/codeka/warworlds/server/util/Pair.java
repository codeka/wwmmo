package au.com.codeka.warworlds.server.util;

import com.google.common.base.Objects;

/**
 * Helper class that represents a pair of values.
 */
public class Pair<E, F> {
  public E one;
  public F two;

  public Pair() {
  }

  public Pair(E one, F two) {
    this.one = one;
    this.two = two;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(one, two);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Pair)) {
      return false;
    }

    Pair<E, F> other = (Pair) obj;
    return Objects.equal(other.one, one) && Objects.equal(other.two, two);
  }
}