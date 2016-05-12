package au.com.codeka;


/**
 * Helper class that represents a pair of values. Unlike \c Pair<>, \c SimplePair cannot
 * be compared (and hence can't be used as a dictionary key).
 */
public class SimplePair<E, F> {
    public E one;
    public F two;

    public SimplePair() {
    }

    public SimplePair(E one, F two) {
        this.one = one;
        this.two = two;
    }
}
