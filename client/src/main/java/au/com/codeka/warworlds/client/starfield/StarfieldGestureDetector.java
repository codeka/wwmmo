package au.com.codeka.warworlds.client.starfield;

import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import au.com.codeka.warworlds.common.Log;

/**
 * Wraps both a {@link GestureDetectorCompat} and a {@link ScaleGestureDetector} and combines
 * them into one set of callbacks that we can more easily respond to.
 */
public class StarfieldGestureDetector {
  private final Log log = new Log("StarfieldGestureDetector");
  private final View view;
  private GestureDetectorCompat gestureDetector;
  private ScaleGestureDetector scaleGestureDetector;

  public StarfieldGestureDetector(View view) {
    this.view = view;
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
      gestureDetector.onTouchEvent(event);
      return true;
    }
  };

  private final GestureDetector.SimpleOnGestureListener gestureListener =
      new GestureDetector.SimpleOnGestureListener() {
    @Override
    public boolean onScroll(
        MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
      log.debug("onScroll(%.2f, %.2f)", distanceX, distanceY);
      return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
        float velocityX, float velocityY) {
      log.debug("onFling(%.2f, %.2f)", velocityX, velocityY);
      return true;
    }
  };

  private final ScaleGestureDetector.SimpleOnScaleGestureListener scaleGestureListener =
      new ScaleGestureDetector.SimpleOnScaleGestureListener() {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      log.debug("onScale(%.2f)", detector.getScaleFactor());
      return true;
    }
  };
}
