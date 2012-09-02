package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import au.com.codeka.Point2D;

public class Sprite {
    private SpriteImage mImage;
    private String mName;
    private Rect mRect;
    private Point2D mUp;

    private static Paint sPaint;

    public Sprite(SpriteImage image, String name) {
        mImage = image;
        mName = name;
        mRect = new Rect();
        mUp = new Point2D(0, 1);
    }

    public String getName() {
        return mName;
    }
    public Rect getRect() {
        return mRect;
    }
    public Point2D getUp() {
        return mUp;
    }
    public int getWidth() {
        return mRect.width();
    }
    public int getHeight() {
        return mRect.height();
    }

    public void draw(Canvas canvas) {
        if (sPaint == null) {
            sPaint = new Paint();
            sPaint.setARGB(255, 255, 255, 255);
            sPaint.setDither(true);
        }

        canvas.drawBitmap(mImage.getBitmap(), mRect,
                          new Rect(0, 0, mRect.width(), mRect.height()), sPaint);
    }

    /**
     * Creates an "icon" of this sprite, which is typically scaled up to the nearest
     * integer multiple of the current size, that's smaller than the given width/height.
     */
    public Bitmap createIcon(int width, int height) {
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
        c.drawBitmap(mImage.getBitmap(), mRect, new RectF(x, y, x + scaleWidth, y + scaleHeight), p);
        return bmp;
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
