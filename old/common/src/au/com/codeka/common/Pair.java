package au.com.codeka.common;

import com.google.common.base.Objects;

/**
 * Helper class that represents a pair of values. This can be used as the key to a dictionary,
 * for example.
 */
public class Pair<E extends Comparable<E>, F extends Comparable<F>>
    implements Comparable<Pair<E, F>> {
  public E one;
  public F two;

  public Pair() {
  }

  public Pair(E one, F two) {
    this.one = one;
    this.two = two;
  }

  @Override
  public int compareTo(Pair<E, F> other) {
    int comp = one.compareTo(other.one);
    if (comp == 0) {
      comp = two.compareTo(other.two);
    }

    return comp;
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
