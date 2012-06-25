package au.com.codeka.planetrender;

import au.com.codeka.planetrender.ObjectPool.Pooled;

/**
 * Represents a 2-dimensional vector.
 */
class Vector2 implements ObjectPool.Pooled {
    public static ObjectPool<Vector2> pool = new ObjectPool<Vector2>(250, new Vector2Creator());

    public double x;
    public double y;

    private Vector2() {
        x = y = 0.0;
    }

    @Override
    public void reset() {
    }

    public Vector2 reset(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Gets the distance squared to the given other point. This is faster than
     * \c distanceTo (since no sqrt() is required) and still good enough for many
     * purposes.
     */
    public double distanceTo2(Vector2 other) {
        final double dx = other.x - x;
        final double dy = other.y - y;
        return dx*dx + dy*dy;
    }

    public double distanceTo(Vector2 other) {
        final double dx = other.x - x;
        final double dy = other.y - y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public double distanceTo2(double x, double y) {
        final double dx = x - this.x;
        final double dy = y - this.y;
        return dx*dx + dy*dy;
    }

    public double distanceTo(double x, double y) {
        final double dx = x - this.x;
        final double dy = y - this.y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public double length2() {
        return (x*x + y*y);
    }
    public double length() {
        return Math.sqrt(length2());
    }

    @Override
    public int hashCode() {
        //?? not very efficient!!
        return (new Double(x).hashCode() ^ new Double(y).hashCode());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Vector2)) {
            return false;
        }

        Vector2 ov = (Vector2) other;
        return (x == ov.x && y == ov.y);
    }

    public boolean equals(Vector2 other, double epsilon) {
        return Math.abs(other.x - x) < epsilon &&
                Math.abs(other.y - y) < epsilon;
    }

    static class Vector2Creator implements ObjectPool.PooledCreator {
        @Override
        public Pooled create() {
            return new Vector2();
        }
    }
}
