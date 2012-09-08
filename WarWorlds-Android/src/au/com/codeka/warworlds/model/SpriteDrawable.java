package au.com.codeka.warworlds.model;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * This is an implementation of \c Drawable that draws a \c Sprite.
 */
public class SpriteDrawable extends Drawable {
    private Sprite mSprite;
    private int mFrameNo;

    public SpriteDrawable(Sprite sprite) {
        mSprite = sprite;
        mFrameNo = 0;
    }
    public SpriteDrawable(Sprite sprite, int frameNo) {
        mSprite = sprite;
        mFrameNo = frameNo;
    }

    @Override
    public void draw(Canvas canvas) {
        mSprite.draw(canvas, mFrameNo);
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
