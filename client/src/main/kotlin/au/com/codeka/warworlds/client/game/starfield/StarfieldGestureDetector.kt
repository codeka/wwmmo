package au.com.codeka.warworlds.client.game.starfield

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.View.OnTouchListener
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ScaleGestureDetectorCompat
import com.google.common.base.Preconditions

/**
 * Wraps both a [GestureDetectorCompat] and a [ScaleGestureDetector] and combines them into one set
 * of callbacks that we can more easily respond to.
 */
class StarfieldGestureDetector(private val view: View, private val callback: Callback) {
  interface Callback {
    /**
     * Called when you scroll the view.
     *
     * @param dx The amount to scrolled in the X direction.
     * @param dy The amount to scrolled in the Y direction.
     */
    fun onScroll(dx: Float, dy: Float)

    /**
     * Called when you fling the view.
     *
     * @param vx The velocity of your fling in the X direction.
     * @param vy The velocity of your fling in the Y direction.
     */
    fun onFling(vx: Float, vy: Float)

    /**
     * Called when you scale the view.
     *
     * @param factor The scaling factor (1.0 == no scale, < 1.0 = shrink, &gt; 1.0 = grow).
     */
    fun onScale(factor: Float)

    /**
     * Called when you tap the view, other than when performing one of the other gestures.
     *
     * @param x The x-coordinate that you tapped.
     * @param y The y-coordinate that you tapped.
     */
    fun onTap(x: Float, y: Float)
  }

  private lateinit var gestureDetector: GestureDetectorCompat
  private lateinit var scaleGestureDetector: ScaleGestureDetector

  fun create() {
    gestureDetector = GestureDetectorCompat(view.context, gestureListener)
    scaleGestureDetector = ScaleGestureDetector(view.context, scaleGestureListener)
    ScaleGestureDetectorCompat.setQuickScaleEnabled(scaleGestureDetector, true)
    view.setOnTouchListener(onTouchListener)
  }

  fun destroy() {
    view.setOnTouchListener(null)
  }

  private val onTouchListener = OnTouchListener { v, event ->
    scaleGestureDetector.onTouchEvent(event)
    if (gestureDetector.onTouchEvent(event)) {
      return@OnTouchListener true
    }
    if (event.action == MotionEvent.ACTION_UP) {
      callback.onTap(event.x, event.y)
      v.performClick()
    }
    true
  }

  private val gestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
    override fun onScroll(
        event1: MotionEvent, event2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
      callback.onScroll(distanceX, distanceY)
      return true
    }

    override fun onFling(event1: MotionEvent, event2: MotionEvent,
                         velocityX: Float, velocityY: Float): Boolean {
      callback.onFling(velocityX, velocityY)
      return true
    }
  }

  private val scaleGestureListener: SimpleOnScaleGestureListener =
      object : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
          callback.onScale(detector.scaleFactor)
          return true
        }
      }
}
