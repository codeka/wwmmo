package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;

/**
 * The \c SelectionView is just two rotating dashed circles.
 */
public class SelectionView extends FrameLayout {
    private CircleView mInnerCircle;
    private CircleView mOuterCircle;

    public SelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public SelectionView(Context context) {
        super(context);
        setup(context);
    }

    private void setup(Context context) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mOuterCircle = new CircleView(context);
        mOuterCircle.setLayoutParams(lp);
        addView(mOuterCircle);

        lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        lp.topMargin = lp.leftMargin = lp.bottomMargin = lp.rightMargin = 6;
        mInnerCircle = new CircleView(context);
        mInnerCircle.setLayoutParams(lp);
        addView(mInnerCircle);
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimation();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateAnimation();
    }

    private void updateAnimation() {
        RotateAnimation outerAnimation = getRotation(0.0f, 360.0f, mOuterCircle);
        mOuterCircle.setAnimation(outerAnimation);
        mOuterCircle.startAnimation(outerAnimation);

        RotateAnimation innerAnimation = getRotation(360.0f, 0.0f, mInnerCircle);
        mInnerCircle.setAnimation(innerAnimation);
        mInnerCircle.startAnimation(innerAnimation);
    }

    private RotateAnimation getRotation(float startAngle, float endAngle, View view) {
        final int pivotX = view.getWidth() / 2;
        final int pivotY = view.getHeight() / 2;

        RotateAnimation anim = new RotateAnimation(startAngle, endAngle, pivotX, pivotY);
        anim.setStartOffset(0);
        anim.setDuration(3000);
        anim.setRepeatCount(Animation.INFINITE);
        anim.setRepeatMode(Animation.RESTART);
        anim.setInterpolator(new LinearInterpolator());
        return anim;
    }

    public class CircleView extends View {
        private Paint mPaint;
        private RectF mBounds;

        public CircleView(Context context) {
            super(context);
        }


        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            updateAnimation();
        }

        @Override
        public void onDraw(Canvas c) {
            int numDashes = 40; // we want 40 dashes around the circle
            float dashAngleDegrees = 360.0f / numDashes;

            if (mPaint == null) {
                mPaint = new Paint();
                mPaint.setARGB(255, 255, 255, 255);
                mPaint.setAntiAlias(true);
                mPaint.setStyle(Paint.Style.STROKE);

                float dashAngleRadians = (float)((2 * Math.PI) / numDashes);
                float dashPixels = dashAngleRadians * ((float) getWidth() / 2.0f);
                DashPathEffect pathEffect = new DashPathEffect(new float[] {
                        dashPixels, dashPixels}, 1);
                mPaint.setPathEffect(pathEffect);
            }

            if (mBounds == null) {
                mBounds = new RectF(0, 0, getWidth(), getHeight());
            }

            // we cannot make the arc 360 degrees, otherwise it ignores the angle offset. Instead
            // we make it 360 - "dash-angle" (i.e. the angle through which one dash passes)
            c.drawArc(mBounds, 0.0f, 360.0f - dashAngleDegrees, false, mPaint);
        }
    }
}
