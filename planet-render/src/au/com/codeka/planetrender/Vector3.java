package au.com.codeka.planetrender;

/**
 * Helper class that represents a 3-dimensional vector.
 */
class Vector3 {

    public double x;
    public double  y;
    public double z;

    public Vector3() {
        x = y = z = 0.0;
    }
    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double length() {
        return Math.sqrt((x*x) + (y*y) + (z*z));
    }

    public void normalize() {
        double s = 1.0 / length();
        scale(s);
    }
    public Vector3 normalized() {
        double s = 1.0 / length();
        return scaled(s);
    }

    public void scale(double s) {
        x *= s;
        y *= s;
        z *= s;
    }
    public Vector3 scaled(double s) {
        return new Vector3(x * s, y * s, z * s);
    }

    /**
     * Returns (a - b)
     */
    public static Vector3 subtract(Vector3 a, Vector3 b) {
        return new Vector3(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static double dot(Vector3 a, Vector3 b) {
        return (a.x * b.x) + (a.y * b.y) + (a.z * b.z);
    }
}
