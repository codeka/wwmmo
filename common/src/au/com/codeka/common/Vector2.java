package au.com.codeka.common;

import java.util.Locale;

import au.com.codeka.common.ObjectPool;
import au.com.codeka.common.ObjectPool.Pooled;

/**
 * Represents a 2-dimensional vector.
 */
public class Vector2 implements ObjectPool.Pooled {
    public static ObjectPool<Vector2> pool = new ObjectPool<Vector2>(250, new Vector2Creator());

    public double x;
    public double y;

    public Vector2() {
        x = y = 0.0;
    }
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public Vector2(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
    }

    @Override
    public void reset() {
    }

    public Vector2 reset(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }
    public Vector2 reset(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
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


    public void add(Vector2 other) {
        x += other.x;
        y += other.y;
    }
    public void add(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public void subtract(Vector2 other) {
        x -= other.x;
        y -= other.y;
    }
    public void subtract(float x, float y) {
        this.x -= x;
        this.y -= y;
    }

    public void rotate(float radians) {
        float nx = (float)(x*Math.cos(radians) - y*Math.sin(radians));
        float ny = (float)(y*Math.cos(radians) + x*Math.sin(radians));
        x = nx;
        y = ny;
    }

    public void normalize() {
        scale(1.0 / length());
    }

    public void scale(double s) {
        x *= s;
        y *= s;
    }

    @Override
    public int hashCode() {
        // this avoids the boxing that "new Double(x).hashCode()" would require
        long lx = Double.doubleToRawLongBits(x);
        long ly = Double.doubleToRawLongBits(y);
        return (int) (lx ^ ly);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "(%.4f, %.4f)", x, y);
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

    /**
     * Find the angle between "a" and "b"
     * see: http://www.gamedev.net/topic/487576-angle-between-two-lines-clockwise/
     */
    public static float angleBetween(Vector2 a, Vector2 b) {
        return (float) Math.atan2(a.x * b.y - a.y * b.x,
                                  a.x * b.x + a.y * b.y);
    }

    public static float angleBetweenCcw(Vector2 a, Vector2 b) {
        return (float) Math.atan2(a.x * b.x + a.y * b.y,
                                  a.x * b.y - a.y * b.x);
    }

    static class Vector2Creator implements ObjectPool.PooledCreator {
        @Override
        public Pooled create() {
            return new Vector2();
        }
    }
}
