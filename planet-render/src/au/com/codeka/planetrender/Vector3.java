package au.com.codeka.planetrender;

import au.com.codeka.planetrender.ObjectPool.Pooled;

/**
 * Helper class that represents a 3-dimensional vector.
 */
public class Vector3 implements ObjectPool.Pooled {
    public static ObjectPool<Vector3> pool = new ObjectPool<Vector3>(250, new Vector3Creator());

    public double x;
    public double y;
    public double z;

    private Vector3() {
    }

    @Override
    public void reset() {
    }

    public Vector3 reset(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 reset(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    public double length() {
        return Math.sqrt((x*x) + (y*y) + (z*z));
    }

    public static double distanceBetween(Vector3 lhs, Vector3 rhs) {
        final double dx = rhs.x - lhs.x;
        final double dy = rhs.y - lhs.y;
        final double dz = rhs.z - lhs.z;

        return Math.sqrt((dx*dx) + (dy*dy) + (dz*dz));
    }

    public void normalize() {
        double s = 1.0 / length();
        scale(s);
    }

    public void rotateX(double radians) {
        double y1 = y * Math.cos(radians) - z * Math.sin(radians);
        double z1 = y * Math.sin(radians) + z * Math.cos(radians);
        y = y1;
        z = z1;
    }
    public void rotateY(double radians) {
        double x1 = x * Math.cos(radians) - z * Math.sin(radians);
        double z1 = x * Math.sin(radians) + z * Math.cos(radians);
        x = x1;
        z = z1;
    }
    public void rotateZ(double radians) {
        double x1 = x * Math.cos(radians) - y * Math.sin(radians);
        double y1 = x * Math.sin(radians) + y * Math.cos(radians);
        x = x1;
        y = y1;
    }

    public void scale(double s) {
        x *= s;
        y *= s;
        z *= s;
    }

    public void subtract(Vector3 rhs) {
        x -= rhs.x;
        y -= rhs.y;
        z -= rhs.z;
    }

    public void add(Vector3 rhs) {
        x += rhs.x;
        y += rhs.y;
        z += rhs.z;
    }

    public static double dot(Vector3 a, Vector3 b) {
        return (a.x * b.x) + (a.y * b.y) + (a.z * b.z);
    }

    public static Vector3 cross(Vector3 a, Vector3 b) {
        return Vector3.pool.borrow().reset(
                            (a.y * b.z) - (a.z * b.y),
                            (a.z * b.x) - (a.x * b.z),
                            (a.x * b.y) - (a.y * b.x));
    }

    public static void interpolate(Vector3 result, Vector3 rhs, double n) {
        result.x += (n * (rhs.x - result.x));
        result.y += (n * (rhs.y - result.y));
        result.z += (n * (rhs.z - result.z));
    }

    static class Vector3Creator implements ObjectPool.PooledCreator {
        @Override
        public Pooled create() {
            return new Vector3();
        }
    }
}
