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
        mInnerCircle.setReverse(true);
        addView(mInnerCircle);
    }

    public class CircleView extends View {
        private Paint mPaint;
        private RectF mBounds;
        private boolean mIsReverse;

        private static final int NUM_DASHES = 40;

        public CircleView(Context context) {
            super(context);
        }

        public void setReverse(boolean reverse) {
            mIsReverse = reverse;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            float size = Math.max(w, h);
            final float pivot = size / 2.0f;
            final float startAngle = (mIsReverse ? 360.0f : 0.0f);
            final float endAngle = (mIsReverse ? 0.0f : 360.0f);

            RotateAnimation anim = new RotateAnimation(startAngle, endAngle, pivot, pivot);
            anim.setStartOffset(0);
            anim.setDuration(4000);
            anim.setRepeatCount(Animation.INFINITE);
            anim.setRepeatMode(Animation.RESTART);
            anim.setInterpolator(new LinearInterpolator());
            setAnimation(anim);
            startAnimation(anim);

            mBounds = new RectF(0, 0, size, size);

            mPaint = new Paint();
            mPaint.setARGB(255, 255, 255, 255);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);

            float dashAngleRadians = (float)((2 * Math.PI) / NUM_DASHES);
            float dashPixels = dashAngleRadians * ((float) getWidth() / 2.0f);
            DashPathEffect pathEffect = new DashPathEffect(new float[] {
                    dashPixels, dashPixels}, 1);
            mPaint.setPathEffect(pathEffect);
        }

        @Override
        public void onDraw(Canvas c) {
            float dashAngleDegrees = 360.0f / NUM_DASHES;

            // we cannot make the arc 360 degrees, otherwise it ignores the angle offset. Instead
            // we make it 360 - "dash-angle" (i.e. the angle through which one dash passes)
            c.drawArc(mBounds, 0.0f, 360.0f - dashAngleDegrees, false, mPaint);
        }
    }
}
