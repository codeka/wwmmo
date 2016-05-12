package au.com.codeka.warworlds.model;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

/**
 * This is an implementation of \c Drawable that draws a \c Sprite.
 */
public class SpriteDrawable extends Drawable {
    private Sprite mSprite;
    private int mFrameNo;
    private int mGravity;
    private boolean mApplyGravity;
    private Rect mGravityRect;

    public SpriteDrawable(Sprite sprite) {
        mSprite = sprite;
        mFrameNo = 0;
    }
    public SpriteDrawable(Sprite sprite, int frameNo) {
        mSprite = sprite;
        mFrameNo = frameNo;
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            mApplyGravity = true;
        }
    }
    public int getGravity() {
        return mGravity;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mApplyGravity) {
            mGravityRect = new Rect();
            Gravity.apply(mGravity, mSprite.getWidth(), mSprite.getHeight(),
                          getBounds(), mGravityRect);
        }
        mSprite.draw(canvas, mFrameNo, mGravityRect);
    }

    @Override
    public int getIntrinsicWidth() {
        return mSprite.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mSprite.getHeight();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }
}
