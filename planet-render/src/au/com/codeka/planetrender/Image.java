package au.com.codeka.planetrender;


/**
 * Represents an image, and provides methods for converting to/from common formats.
 */
public class Image {
    private int[] mArgb;
    private int mWidth;
    private int mHeight;

    public Image(int width, int height) {
        mWidth = width;
        mHeight = height;
        mArgb = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mArgb[y * width + x] = 0xff000000;
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
        mArgb[y * mWidth + x] = c.argb;
    }
}
