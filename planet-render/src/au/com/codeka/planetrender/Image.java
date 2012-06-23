package au.com.codeka.planetrender;


/**
 * Represents an image, and provides methods for converting to/from common formats.
 */
public class Image {
    private int[] mArgb;
    private int mWidth;
    private int mHeight;

    public Image(int width, int height) {
        this(width, height, Colour.BLACK);
    }
    public Image(int width, int height, Colour fill) {
        final int argb = fill.toArgb();

        mWidth = width;
        mHeight = height;
        mArgb = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mArgb[y * width + x] = argb;
            }
        }
    }

    public int getWidth() {
        return mWidth;
    }
    public int getHeight() {
        return mHeight;
    }
    public int[] getArgb() {
        return mArgb;
    }

    public void setPixelColour(int x, int y, Colour c) {
        if (x < 0 || x >= mWidth)
            return;
        if (y < 0 || y >= mHeight)
            return;

        mArgb[y * mWidth + x] = c.toArgb();
    }

    /**
     * Draws a circle at the given (x,y) coordinates with the given radius and colour.
     */
    public void drawCircle(int cx, int cy, double radius, Colour c) {
        Vector2 centre = new Vector2(cx, cy);
        for (int y = (int)(cy - radius); y <= (int)(cy + radius); y++) {
            for (int x = (int)(cx - radius); x <= (int)(cx + radius); x++) {
                Vector2 p = new Vector2(x, y);
                double distance = p.distanceTo(centre); 
                if (distance < radius) {
                    setPixelColour(x, y, c);
                }
            }
        }
    }

    /**
     * Draws a line from the given (x1,y1) to (x2,y2) in the given colour.
     */
    public void drawLine(int x1, int y1, int x2, int y2, Colour c) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int sx = (x1 < x2 ? 1 : -1);
        int sy = (y1 < y2 ? 1 : -1);
        int err = dx - dy;

        while(true) {
            setPixelColour(x1, y1, c);
            if (x1 == x2 && y1 == y2)
                break;

            int e2 = 2*err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
}
