package au.com.codeka;

/**
 * Represents a point in 2D space, with some convenience methods for manipulating them.
 */
public class Point2D {
    private double mX;
    private double mY;

    public Point2D() {
        mX = mY = 0.0;
    }

    public Point2D(int x, int y) {
        mX = x;
        mY = y;
    }

    public Point2D(double x, double y) {
        mX = x;
        mY = y;
    }

    public double getX() {
        return mX;
    }
    public void setX(double x) {
        mX = x;
    }
    public double getY() {
        return mY;
    }
    public void setY(double y) {
        mY = y;
    }

    public Point2D add(final Point2D other) {
        return new Point2D(
                mX + other.mX,
                mY + other.mY);
    }

    public Point2D subtract(final Point2D other) {
        return new Point2D(
                mX - other.mX,
                mY - other.mY);
    }

    public double scalarLength() {
        return Math.sqrt(mX*mX + mY*mY);
    }

    public double distanceTo(Point2D other) {
        return other.subtract(this).scalarLength();
    }

    public Point2D rotate(double radians) {
        double nx = mX*Math.cos(radians) - mY*Math.sin(radians);
        double ny = mY*Math.cos(radians) + mX*Math.sin(radians);
        return new Point2D(nx, ny);
    }
}
