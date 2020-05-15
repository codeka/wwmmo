package au.com.codeka.warworlds.client.opengl

import android.opengl.Matrix
import android.view.animation.DecelerateInterpolator

/** The [Camera] can be used to draw a [Scene], scroll and zoom around. */
class Camera {
  interface CameraUpdateListener {
    /**
     * Called when the camera is translated.
     *
     * @param x The total distance the camera is translated away from the origin in the X direction.
     * @param y The total distance the camera is translated away from the origin in the Y direction.
     * @param dx The delta the camera has moved in the X direction.
     * @param dy The delta the camera has moved in the Y direction.
     */
    fun onCameraTranslate(x: Float, y: Float, dx: Float, dy: Float)
  }

  private val projMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)

  val viewProjMatrix = FloatArray(16)
    get() {
      Matrix.setIdentityM(viewMatrix, 0)
      Matrix.scaleM(viewMatrix, 0, zoomAmount, zoomAmount, 1.0f)
      Matrix.translateM(viewMatrix, 0, translateX, translateY, 0.0f)
      Matrix.multiplyMM(field, 0, projMatrix, 0, viewMatrix, 0)
      return field
    }

  var zoomAmount = 0f
    private set
  private var flinging = false
  private var flingX = 0f
  private var flingY = 0f
  private var flingFactor = 0f
  private val decelerateInterpolator = DecelerateInterpolator(1.0f)
  var screenWidth = 0f
    private set
  var screenHeight = 0f
    private set
  private var translateX = 0f
  private var translateY = 0f
  private var listener: CameraUpdateListener? = null

  fun setCameraUpdateListener(listener: CameraUpdateListener?) {
    this.listener = listener
  }

  fun onSurfaceChanged(width: Float, height: Float) {
    Matrix.orthoM(projMatrix, 0, -width, width, -height, height, 10f, -10f)
    zoomAmount = 1.0f
    flinging = false
    screenWidth = width
    screenHeight = height
    zoom(2.0f)
  }

  fun onDraw() {
    if (flinging) {
      val dt = 0.016f
      flingFactor += dt * 2.0f
      if (flingFactor > 1.0f) {
        flinging = false
      } else {
        val factor = 1.0f - decelerateInterpolator.getInterpolation(flingFactor)
        translate(flingX * factor * dt, flingY * factor * dt)
      }
    }
  }

  /**
   * Translate by the given amount.
   *
   * @param x Amount to translate in X direction.
   * @param y Amount to translate in Y direction.
   * @param silent If true, we will not call the listener.
   */
  fun translate(x: Float, y: Float, silent: Boolean = false) {
    val scaledX = x / (zoomAmount * 0.5f)
    val scaledY = y / (zoomAmount * 0.5f)
    translateX += scaledX
    translateY += scaledY
    if (listener != null && !silent) {
      listener!!.onCameraTranslate(-translateX, translateY, scaledX, scaledY)
    }
  }

  /**
   * Similar to [.translate] except that we translate to an absolute
   * X,Y coordinate.
   *
   * @param x The X-coordinate you want to warp to.
   * @param y The Y-coordinate you want to warp to.
   */
  fun warpTo(x: Float, y: Float) {
    translateX = x
    translateY = y
  }

  fun fling(x: Float, y: Float) {
    flinging = true
    flingX = x
    flingY = y
    flingFactor = 0.0f
  }

  fun zoom(factor: Float) {
    zoomAmount *= factor
    if (zoomAmount > 5.0f) {
      zoomAmount = 5.0f
    }
    if (zoomAmount < 0.25f) {
      zoomAmount = 0.25f
    }
  }

  init {
    Matrix.setIdentityM(viewMatrix, 0)
  }
}