package au.com.codeka;

/**
 * Represents a point in 2D space, with some convenience methods for manipulating them.
 */
public class Point2D {
    private float mX;
    private float mY;

    public Point2D() {
        mX = mY = 0.0f;
    }

    public Point2D(int x, int y) {
        mX = x;
        mY = y;
    }

    public Point2D(float x, float y) {
        mX = x;
        mY = y;
    }

    public float getX() {
        return mX;
    }
    public void setX(float x) {
        mX = x;
    }
    public float getY() {
        return mY;
    }
    public void setY(float y) {
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

    public Point2D rotate(float radians) {
        float nx = (float)(mX*Math.cos(radians) - mY*Math.sin(radians));
        float ny = (float)(mY*Math.cos(radians) + mX*Math.sin(radians));
        return new Point2D(nx, ny);
    }
}
