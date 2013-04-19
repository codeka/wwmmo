package au.com.codeka;

import android.util.FloatMath;

/**
 * Represents a point in 2D space, with some convenience methods for manipulating them.
 */
public class Point2D {
    public float x;
    public float y;

    public Point2D() {
        x = y = 0.0f;
    }

    public Point2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Point2D(Point2D other) {
        this.x = other.x;
        this.y = other.y;
    }

    public void add(Point2D other) {
        x += other.x;
        y += other.y;
    }
    public void add(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public void subtract(Point2D other) {
        x -= other.x;
        y -= other.y;
    }
    public void subtract(float x, float y) {
        this.x -= x;
        this.y -= y;
    }

    public float scalarLength() {
        return FloatMath.sqrt(x*x + y*y);
    }

    public float distanceTo(Point2D other) {
        Point2D tmp = new Point2D(this.x, this.y);
        tmp.subtract(other);
        return tmp.scalarLength();
    }

    public void rotate(float radians) {
        float nx = (float)(x*FloatMath.cos(radians) - y*FloatMath.sin(radians));
        float ny = (float)(y*FloatMath.cos(radians) + x*FloatMath.sin(radians));
        x = nx;
        y = ny;
    }

    public void normalize() {
        scale(1.0f / scalarLength());
    }

    public void scale(float s) {
        x *= s;
        y *= s;
    }

    // 
    /**
     * Find the angle between "a" and "b"
     * see: http://www.gamedev.net/topic/487576-angle-between-two-lines-clockwise/
     */
    public static float angleBetween(Point2D a, Point2D b) {
        return (float) Math.atan2(a.x * b.y - a.y * b.x,
                                  a.x * b.x + a.y * b.y);
    }
}
