package au.com.codeka.warworlds.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import au.com.codeka.common.Vector2;
import au.com.codeka.warworlds.App;

public class Sprite {
    private SpriteImage mImage;
    private String mName;
    private ArrayList<SpriteFrame> mFrames;
    private Vector2 mUp;
    private int mWidth;
    private int mHeight;
    private float mScale;

    private static Paint sPaint;

    public Sprite(SpriteImage image, String name) {
        mImage = image;
        mName = name;
        mFrames = new ArrayList<SpriteFrame>();
        mUp = new Vector2(0, 1);
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
    public Vector2 getUp() {
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

    public void draw(Canvas canvas) {
        draw(canvas, 0, null);
    }

    public void draw(Canvas canvas, int frameNo) {
        draw(canvas, 0, null);
    }

    public void draw(Canvas canvas, int frameNo, Rect destRect) {
        if (sPaint == null) {
            sPaint = new Paint();
            sPaint.setARGB(255, 255, 255, 255);
            sPaint.setDither(true);
            sPaint.setFilterBitmap(true);
        }

        SpriteFrame frame = getFrame(frameNo);
        if (mScale == 1.0f || destRect != null) {
            if (destRect == null) {
                destRect = new Rect(0, 0, frame.getWidth(), frame.getHeight());
            }
            canvas.drawBitmap(mImage.getBitmap(), frame.getRect(),
                              destRect, sPaint);
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
        private boolean mIsAsset;
        private String mFileName;
        private SoftReference<Bitmap> mSoftBitmap;
        private Rect mDimensions;

        public String getFileName() {
            return mFileName;
        }
        public Bitmap getBitmap() {
            Bitmap bmp = null;
            if (mSoftBitmap != null) {
                bmp = mSoftBitmap.get();
            }
            if (bmp == null && mFileName != null) {
                bmp = loadBitmap();
                mSoftBitmap = new SoftReference<Bitmap>(bmp);
            }

            return bmp;
        }

        public SpriteImage(String fileName, boolean isAsset) {
            mFileName = fileName;
            mIsAsset = isAsset;
        }

        public int getWidth() {
            if (mDimensions == null) {
                calcDimensions();
            }
            return mDimensions.width();
        }

        public int getHeight() {
            if (mDimensions == null) {
                calcDimensions();
            }
            return mDimensions.height();
        }

        private void calcDimensions() {
            // if we have a bitmap in memory already, just use that
            Bitmap bmp = null;
            if (mSoftBitmap != null) {
                bmp = mSoftBitmap.get();
            }
            if (bmp != null) {
                mDimensions = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
            }

            // otherwise, avoid loading the whole bitmap and just return the
            // dimenions if possible.
            try {
                InputStream ins = load();
                Options opt = new Options();
                opt.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(ins, null, opt);
                mDimensions = new Rect(0, 0, opt.outWidth, opt.outHeight);
            } catch(IOException e) {
                throw new RuntimeException("Error fetching dimensions of: "+mFileName, e);
            }
        }

        private Bitmap loadBitmap() {
            InputStream ins = null;
            try {
                ins = load();

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPurgeable = true;
                opts.inInputShareable = true;

                return BitmapFactory.decodeStream(ins, null, opts);
            } catch (IOException e) {
                return null;
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch(Exception e) {}
                }
            }
        }

        private InputStream load() throws IOException {
            if (mIsAsset) {
                return App.i.getAssets().open(mFileName);
            } else {
                return new FileInputStream(mFileName);
            }
        }
    }
}
