package au.com.codeka.warworlds.client.opengl

import android.opengl.Matrix
import android.view.animation.DecelerateInterpolator
import au.com.codeka.warworlds.client.game.starfield.scene.StarfieldManager

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

  private var viewProjMatrixDirty = true
  val viewProjMatrix = FloatArray(16)
    get() {
      if (viewProjMatrixDirty) {
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.scaleM(viewMatrix, 0, zoomAmount, zoomAmount, 1.0f)
        Matrix.translateM(viewMatrix, 0, translateX, translateY, 0.0f)
        Matrix.multiplyMM(field, 0, projMatrix, 0, viewMatrix, 0)

        Matrix.invertM(inverseViewProjMatrix, 0, field, 0)
      }

      return field
    }

  /**
   * The inverse of the viewProjMatrix, useful for projecting from screen coordinates to world
   * coordinates, for example.
   */
  var inverseViewProjMatrix = FloatArray(16)
    private set

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

  /** "Unproject" the given screen-space x,y into world coordinates. */
  fun unproject(x: Float, y: Float): FloatArray {
    var inputVector = FloatArray(4)
    inputVector[0] = ((x / screenWidth) - 0.5f) * 2.0f
    inputVector[1]  = -((y / screenHeight) - 0.5f) * 2.0f
    inputVector[2] = 0.0f
    inputVector[3] = 1.0f
    var outputVector = FloatArray(4)
    Matrix.multiplyMV(outputVector, 0, inverseViewProjMatrix, 0, inputVector, 0)
    return outputVector
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
    viewProjMatrixDirty = true
    listener?.let {
      if (!silent) {
        it.onCameraTranslate(-translateX, translateY, scaledX, scaledY)
      }
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
    viewProjMatrixDirty = true
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
    viewProjMatrixDirty = true
  }

  init {
    Matrix.setIdentityM(viewMatrix, 0)
  }
}