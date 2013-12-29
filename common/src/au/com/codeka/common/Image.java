package au.com.codeka.common;


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

    public void blendPixelColour(int x, int y, Colour c) {
        if (x < 0 || x >= mWidth)
            return;
        if (y < 0 || y >= mHeight)
            return;

        Colour imgColour = Colour.pool.borrow().reset(mArgb[y * mWidth + x]);
        Colour.blend(imgColour, c);
        mArgb[y * mWidth + x] = imgColour.toArgb();
    }

    /**
     * Draws a circle at the given (x,y) coordinates with the given radius and colour.
     */
    public void drawCircle(int cx, int cy, double radius, Colour c) {
        Vector2 centre = Vector2.pool.borrow();
        centre.x = cx; centre.y = cy;
        for (int y = (int)(cy - radius); y <= (int)(cy + radius); y++) {
            for (int x = (int)(cx - radius); x <= (int)(cx + radius); x++) {
                double distance = centre.distanceTo(x, y); 
                if (distance < radius) {
                    setPixelColour(x, y, c);
                }
            }
        }
        Vector2.pool.release(centre);
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

    /**
     * Draws the given triangle to this image.
     * 
     * http://joshbeam.com/articles/triangle_rasterization/
     */
    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Colour c) {
        TriangleEdge[] edges = {
                new TriangleEdge(x1, y1, x2, y2),
                new TriangleEdge(x2, y2, x3, y3),
                new TriangleEdge(x3, y3, x1, y1)
            };

        // find out which edge is the tallest
        int maxLength = 0;
        int longEdge = 0;
        for (int i = 0; i < 3; i++) {
            int length = edges[i].y2 - edges[i].y1;
            if (length > maxLength) {
                maxLength = length;
                longEdge = i;
            }
        }

        drawSpansBetweenEdges(edges[longEdge], edges[(longEdge + 1) % 3], c);
        drawSpansBetweenEdges(edges[longEdge], edges[(longEdge + 2) % 3], c);
    }

    /**
     * This is basically a modification on bresenham's algroithm we use for drawLine()
     */
    private void drawSpansBetweenEdges(TriangleEdge longEdge, TriangleEdge shortEdge, Colour c) {
        int x11 = longEdge.x1;
        int x12 = longEdge.x2;
        int x21 = shortEdge.x1;
        int x22 = shortEdge.x2;
        int y11 = longEdge.y1;
        int y12 = longEdge.y2;
        int y21 = shortEdge.y1;
        int y22 = shortEdge.y2;

        int dx1 = Math.abs(x12 - x11);
        int dy1 = Math.abs(y12 - y11);
        int dx2 = Math.abs(x22 - x21);
        int dy2 = Math.abs(y22 - y21);

        int sx1 = (x11 < x12 ? 1 : -1);
        int sy1 = (y11 < y12 ? 1 : -1);
        int sx2 = (x21 < x22 ? 1 : -1);
        int sy2 = (y21 < y22 ? 1 : -1);
        int err1 = dx1 - dy1;
        int err2 = dx2 - dy2;

        while(true) {
            if (y11 == y21) {
                int startX = x11;
                int endX = x21;
                if (startX > endX) {
                    int tmp = startX;
                    startX = endX;
                    endX = tmp;
                }

                for (int x = startX; x <= endX; x++) {
                    setPixelColour(x, y11, c);
                }
            }

            if (x11 == x12 && y11 == y12)
                return;
            if (x21 == x22 && y21 == y22)
                return;

            int e12 = 2*err1;
            if (e12 > -dy1) {
                err1 -= dy1;
                x11 += sx1;
            }
            if (e12 < dx1) {
                err1 += dx1;
                y11 += sy1;
            }

            while (y11 > y21) {
                int e22 = 2*err2;
                if (e22 > -dy2) {
                    err2 -= dy2;
                    x21 += sx2;
                }
                if (e22 < dx2) {
                    err2 += dx2;
                    y21 += sy2;
                }

                if (x21 == x22 && y21 == y22)
                    return;
            }
        }
    }

    private static class TriangleEdge {
        public int x1;
        public int y1;
        public int x2;
        public int y2;

        public TriangleEdge(int x1, int y1, int x2, int y2) {
            if (y1 < y2) {
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
            } else {
                this.x1 = x2;
                this.y1 = y2;
                this.x2 = x1;
                this.y2 = y1;
            }
        }
    }
}
