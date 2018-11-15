package au.com.codeka.warworlds.client.game.starfield;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ScaleGestureDetectorCompat;

import com.google.common.base.Preconditions;

/**
 * Wraps both a {@link GestureDetectorCompat} and a {@link ScaleGestureDetector} and combines
 * them into one set of callbacks that we can more easily respond to.
 */
public class StarfieldGestureDetector {
  public interface Callback {
    /**
     * Called when you scroll the view.
     *
     * @param dx The amount to scrolled in the X direction.
     * @param dy The amount to scrolled in the Y direction.
     */
    void onScroll(float dx, float dy);

    /**
     * Called when you fling the view.
     *
     * @param vx The velocity of your fling in the X direction.
     * @param vy The velocity of your fling in the Y direction.
     */
    void onFling(float vx, float vy);

    /**
     * Called when you scale the view.
     *
     * @param factor The scaling factor (1.0 == no scale, < 1.0 = shrink, &gt; 1.0 = grow).
     */
    void onScale(float factor);

    /**
     * Called when you tap the view, other than when performing one of the other gestures.
     *
     * @param x The x-coordinate that you tapped.
     * @param y The y-coordinate that you tapped.
     */
    void onTap(float x, float y);
  }

  private final View view;
  private final Callback callback;
  private GestureDetectorCompat gestureDetector;
  private ScaleGestureDetector scaleGestureDetector;

  public StarfieldGestureDetector(View view, Callback callback) {
    this.view = Preconditions.checkNotNull(view);
    this.callback = Preconditions.checkNotNull(callback);
  }

  public void create() {
    gestureDetector = new GestureDetectorCompat(view.getContext(), gestureListener);
    scaleGestureDetector = new ScaleGestureDetector(view.getContext(), scaleGestureListener);
    ScaleGestureDetectorCompat.setQuickScaleEnabled(scaleGestureDetector, true);
    view.setOnTouchListener(onTouchListener);
  }

  public void destroy() {
    view.setOnTouchListener(null);
    scaleGestureDetector = null;
    gestureDetector = null;
  }

  private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      scaleGestureDetector.onTouchEvent(event);
      if (gestureDetector.onTouchEvent(event)) {
        return true;
      }

      if (event.getAction() == MotionEvent.ACTION_UP) {
        callback.onTap(event.getX(), event.getY());
      }

      return true;
    }
  };

  private final GestureDetector.SimpleOnGestureListener gestureListener =
      new GestureDetector.SimpleOnGestureListener() {
    @Override
    public boolean onScroll(
        MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
      callback.onScroll(distanceX, distanceY);
      return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
        float velocityX, float velocityY) {
      callback.onFling(velocityX, velocityY);
      return true;
    }
  };

  private final ScaleGestureDetector.SimpleOnScaleGestureListener scaleGestureListener =
      new ScaleGestureDetector.SimpleOnScaleGestureListener() {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      callback.onScale(detector.getScaleFactor());
      return true;
    }
  };
}
