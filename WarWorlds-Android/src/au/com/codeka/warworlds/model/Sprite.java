package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import au.com.codeka.Point2D;

/**
 * A sprite is a \c Drawable that represents a subsection of a larger image. It also (optionally)
 * supports animation (via the frames collection).
 */
public class Sprite extends Drawable {
    private SpriteImage mImage;
    private String mName;
    private ArrayList<SpriteFrame> mFrames;
    private Point2D mUp;
    private int mWidth;
    private int mHeight;
    private float mScale;

    private static Paint sPaint;

    public Sprite(SpriteImage image, String name) {
        mImage = image;
        mName = name;
        mFrames = new ArrayList<SpriteFrame>();
        mUp = new Point2D(0, 1);
        mScale = 1.0f;
    }

    public String getName() {
        return mName;
    }
    public SpriteFrame getFrame(int n) {
        if (n < 0 || n >= mFrames.size()) {
            return mFrames.get(0);
        }
        return mFrames.get(n);
    }
    public int getNumFrames() {
        return mFrames.size();
    }
    public Point2D getUp() {
        return mUp;
    }
    public int getWidth() {
        return mWidth;
    }
    public int getHeight() {
        return mHeight;
    }
    public float getScale() {
        return mScale;
    }
    public void setScale(float scale) {
        mScale = scale;
    }

    public void addFrame(SpriteFrame frame) {
        mFrames.add(frame);
        if (mWidth < frame.getWidth()) {
            mWidth = frame.getWidth();
        }
        if (mHeight < frame.getHeight()) {
            mHeight = frame.getHeight();
        }
    }

    /**
     * "Extracts" the given frame from this sprite and returns a new one-frame
     * sprite with that frame.
     */
    public Sprite extractFrame(int frameNo) {
        Sprite copy = new Sprite(mImage, mName);
        copy.addFrame(getFrame(frameNo));
        copy.mUp = mUp;
        return copy;
    }

    /**
     * Creates a "simple" sprite directly from the given bitmap. The sprite will just be
     * the whole bitmap.
     */
    public static Sprite createSimpleSprite(Bitmap bmp) {
        SpriteImage img = new SpriteImage(bmp);
        Sprite sprite = new Sprite(img, "simple");
        sprite.addFrame(new SpriteFrame(new Rect(0, 0, bmp.getWidth(), bmp.getHeight())));
        return sprite;
    }

    @Override
    public void draw(Canvas canvas) {
        draw(canvas, 0);
    }
    public void draw(Canvas canvas, int frameNo) {
        if (sPaint == null) {
            sPaint = new Paint();
            sPaint.setARGB(255, 255, 255, 255);
            sPaint.setDither(true);
        }

        SpriteFrame frame = getFrame(frameNo);
        if (mScale == 1.0f) {
            canvas.drawBitmap(mImage.getBitmap(), frame.getRect(),
                              new Rect(0, 0, frame.getWidth(), frame.getHeight()), sPaint);
        } else {
            float oneMinusScale = 1.0f - mScale;
            int sideBorder = (int)(oneMinusScale * 0.5f * frame.getWidth());
            int topBorder = (int)(oneMinusScale * 0.5f * frame.getHeight());
            Rect r = new Rect(sideBorder,
                              topBorder,
                              frame.getWidth() - sideBorder,
                              frame.getHeight() - sideBorder);

            canvas.drawBitmap(mImage.getBitmap(), frame.getRect(), r, sPaint);
        }
    }

    @Override
    public int getOpacity() {
        // TODO Auto-generated method stub
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // TODO Auto-generated method stub
    }

    /**
     * Creates an "icon" of this sprite, which is typically scaled up to the nearest
     * integer multiple of the current size, that's smaller than the given width/height.
     */
    public Bitmap createIcon(int width, int height) {
        return createIcon(width, height, 0);
    }

    public Bitmap createIcon(int width, int height, int frameNo) {
        SpriteFrame frame = getFrame(frameNo);

        int scaleWidth = getWidth();
        int scaleHeight = getHeight();
        int factor = 1;
        while ((scaleWidth * factor) < width && (scaleHeight * factor) < height) {
            factor += 1;
        }
        if (factor > 1) {
            factor --;
        }
        if (factor > 4) {
            factor = 4;
        }
        scaleWidth *= factor;
        scaleHeight *= factor;

        Paint p = new Paint();
        p.setARGB(255, 255, 255, 255);
        p.setDither(false);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.TRANSPARENT);

        float x = (width / 2.0f) - (scaleWidth / 2.0f);
        float y = (height / 2.0f) - (scaleHeight / 2.0f);
        c.drawBitmap(mImage.getBitmap(), frame.getRect(),
                     new RectF(x, y, x + scaleWidth, y + scaleHeight), p);
        return bmp;
    }

    public static class SpriteFrame {
        private Rect mRect;

        public Rect getRect() {
            return mRect;
        }

        public SpriteFrame(Rect rect) {
            mRect = rect;
        }

        public int getWidth() {
            return mRect.width();
        }
        public int getHeight() {
            return mRect.height();
        }
    }

    /**
     * Represents an image that contains (possibly) more than one \c Sprite.
     */
    public static class SpriteImage {
        private String mFileName;
        private Bitmap mBitmap;

        public String getFileName() {
            return mFileName;
        }
        public Bitmap getBitmap() {
            return mBitmap;
        }

        public SpriteImage(Bitmap bmp) {
            mBitmap = bmp;
            mFileName = "";
        }

        public SpriteImage(Context context, String fileName) {
            mFileName = fileName;

            InputStream ins;
            try {
                ins = context.getAssets().open(fileName);
                mBitmap = BitmapFactory.decodeStream(ins);
            } catch (IOException e) {
                // TODO: errors? shouldn't happen...
            }
        }
    }
}
