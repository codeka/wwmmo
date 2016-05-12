package au.com.codeka.common;

/**
 * A Tuple contains two non-comparable items.
 */
public class Tuple<E, F> {
  public E one;
  public F two;

  public Tuple() {
  }

  public Tuple(E one, F two) {
    this.one = one;
    this.two = two;
  }
}
