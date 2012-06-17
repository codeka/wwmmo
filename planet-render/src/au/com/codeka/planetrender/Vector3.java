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
    public Vector3 scaled(double s) {
        return new Vector3(x * s, y * s, z * s);
    }

    public static Vector3 subtract(Vector3 a, Vector3 b) {
        return new Vector3(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vector3 add(Vector3 a, Vector3 b) {
        return new Vector3(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vector3 scale(Vector3 v, double n) {
        return new Vector3(v.x * n, v.y * n, v.z * n);
    }

    public static double dot(Vector3 a, Vector3 b) {
        return (a.x * b.x) + (a.y * b.y) + (a.z * b.z);
    }

    public static Vector3 cross(Vector3 a, Vector3 b) {
        return new Vector3((a.y * b.z) - (a.z * b.y),
                            (a.z * b.x) - (a.x * b.z),
                            (a.x * b.y) - (a.y * b.x));
    }

    public static Vector3 interpolate(Vector3 a, Vector3 b, double n) {
        return new Vector3(
                a.x + (n * (b.x - a.x)),
                a.y + (n * (b.y - a.y)),
                a.z + (n * (b.z - a.z))
            );
    }
}
