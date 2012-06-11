package au.com.codeka.planetrender;

/**
 * Represents a 2-dimensional vector.
 */
class Vector2 {
    public double x;
    public double y;

    public Vector2() {
        x = y = 0.0;
    }
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double distanceTo(Vector2 other) {
        final double dx = other.x - x;
        final double dy = other.y - y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public double length2() {
        return (x*x + y*y);
    }
    public double length() {
        return Math.sqrt(length2());
    }
}
