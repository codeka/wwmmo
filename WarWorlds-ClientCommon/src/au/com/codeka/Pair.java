package au.com.codeka;

/**
 * Helper class that represents a pair of values. This can be used as the key
 * to a dictionary, for example.
 */
public class Pair<
                E extends Comparable<E>,
                F extends Comparable<F>
        > implements Comparable<Pair<E,F>> {
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
}
