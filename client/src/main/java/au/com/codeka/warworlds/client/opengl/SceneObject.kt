package au.com.codeka.warworlds.client.opengl

import android.opengl.Matrix
import au.com.codeka.warworlds.common.Log
import com.google.common.base.Preconditions
import java.util.*

/** Base class for any "object" within a [Scene].  */
open class SceneObject @JvmOverloads constructor(dimensionResolver: DimensionResolver, scene: Scene? = null) {
  private val dimensionResolver: DimensionResolver

  /** The scene we belong to, or null if we're not part of a scene.  */
  var scene: Scene?
    private set

  /** Our parent [SceneObject], if any.  */
  var parent: SceneObject? = null
    private set

  /** Children array will be null until you add the first child.  */
  private var children: ArrayList<SceneObject>? = null

  /** Matrix transform that transforms this scene object into world space.  */
  @JvmField
  protected val matrix = FloatArray(16)
  protected val modelViewProjMatrix = FloatArray(16)

  /**
   * Temporary 4-component vector used in clipping calculations, must be called inside scene lock.
   */
  private val clipVector = FloatArray(8)
  private var widthPx: Float
  private var heightPx: Float
  private var clipRadius: Float? = null

  /**
   * Gets the tap target radius, or `null` if it hasn't been set yet.
   */
  var tapTargetRadius: Float? = null
    private set
  /** Gets the "tag" you previously set in [.setTag].  */
  /**
   * Sets this [SceneObject]'s "tag", which is just an arbitrary object we'll hang onto for
   * you.
   */
  var tag: Any? = null

  /** An optional [Runnable] that'll be called before this [SceneObject] is drawn.  */
  private var drawRunnable: Runnable? = null

  /**
   * Sets an optional draw [Runnable] that is called on the draw thread before this
   * [SceneObject] is drawn. You can use this to update the position, scale, etc of the
   * object just before it's drawn (useful for animation, etc).
   *
   *
   * You cannot modify the object heirarchy in this method (add children, remove children, etc)
   */
  fun setDrawRunnable(drawRunnable: Runnable?) {
    this.drawRunnable = drawRunnable
  }

  fun addChild(child: SceneObject) {
    if (child.parent != null) {
      child.parent!!.removeChild(child)
    }
    if (children == null) {
      children = ArrayList()
    }
    children!!.add(child)
    child.scene = scene
    child.parent = this
  }

  fun removeChild(child: SceneObject) {
    Preconditions.checkState(child.parent === this, "%s != %s", child.parent, this)
    if (children != null) {
      children!!.remove(child)
      child.scene = null
      child.parent = null
    }
  }

  fun removeAllChildren() {
    if (children != null) {
      children!!.clear()
    }
  }

  val numChildren: Int
    get() = if (children == null) {
      0
    } else children!!.size

  fun getChild(index: Int): SceneObject? {
    if (children == null) {
      return null
    }
    return if (index < 0 || index >= children!!.size) {
      null
    } else children!![index]
  }

  /**
   * If non-zero, assume this object is the given radius and refrain from drawing if a circle at
   * our center and with the given radius would be off-screen.
   */
  fun setClipRadius(radius: Float) {
    clipRadius = radius
  }

  /**
   * Sets the tap-target radius, in dp, of this [SceneObject]. If you set this to a non-zero
   * value, then this [SceneObject] will be considered a tap target that you can tap on.
   */
  fun setTapTargetRadius(radius: Float) {
    tapTargetRadius = radius
  }

  fun setSize(widthDp: Float, heightDp: Float) {
    val widthPx = dimensionResolver.dp2px(widthDp)
    val heightPx = dimensionResolver.dp2px(heightDp)
    Matrix.scaleM(matrix, 0, widthPx / this.widthPx, heightPx / this.heightPx, 1.0f)
    this.widthPx = widthPx
    this.heightPx = heightPx
  }

  fun setTranslation(xDp: Float, yDp: Float) {
    val xPx = dimensionResolver.dp2px(xDp)
    val yPx = dimensionResolver.dp2px(yDp)
    matrix[12] = 0f
    matrix[13] = 0f
    matrix[14] = 0f
    matrix[15] = 1.0f
    Matrix.translateM(matrix, 0, xPx / widthPx, yPx / heightPx, 0.0f)
  }

  fun translate(xDp: Float, yDp: Float) {
    val xPx = dimensionResolver.dp2px(xDp)
    val yPx = dimensionResolver.dp2px(yDp)
    Matrix.translateM(matrix, 0, xPx, yPx, 0.0f)
  }

  fun rotate(radians: Float, x: Float, y: Float, z: Float) {
    Matrix.rotateM(matrix, 0, (radians * 180.0f / Math.PI).toFloat(), x, y, z)
  }

  fun setRotation(radians: Float, x: Float, y: Float, z: Float) {
    val tx = matrix[12]
    val ty = matrix[13]
    Matrix.setRotateM(matrix, 0, (radians * 180.0f / Math.PI).toFloat(), x, y, z)
    Matrix.scaleM(matrix, 0, widthPx, heightPx, 1.0f)
    setTranslation(tx, ty)
  }

  fun draw(viewProjMatrix: FloatArray?) {
    val localDrawRunnable = drawRunnable
    localDrawRunnable?.run()
    Matrix.multiplyMM(modelViewProjMatrix, 0, viewProjMatrix, 0, matrix, 0)
    if (clipRadius != null) {
      clipVector[4] = clipRadius ?: 0f
      clipVector[5] = 0.0f
      clipVector[6] = 0.0f
      clipVector[7] = 1.0f
      Matrix.multiplyMV(clipVector, 0, modelViewProjMatrix, 0, clipVector, 4)
      var transformedRadius = clipVector[0]
      project(modelViewProjMatrix, clipVector, 0)
      transformedRadius -= clipVector[0]
      if (clipVector[0] < -1.0f - transformedRadius || clipVector[0] > 1.0f + transformedRadius || clipVector[1] < -1.0f - transformedRadius || clipVector[1] > 1.0f + transformedRadius) {
        // it's outside of the frustum, clip
        return
      }
    }
    drawImpl(modelViewProjMatrix)
    if (children != null) {
      for (i in children!!.indices) {
        children!![i].draw(modelViewProjMatrix)
      }
    }
  }

  /**
   * Projects this [SceneObject], given the view-proj matrix and returns the clip-space
   * coords in `outVec`.
   */
  fun project(viewProjMatrix: FloatArray?, outVec: FloatArray) {
    Matrix.multiplyMM(modelViewProjMatrix, 0, viewProjMatrix, 0, matrix, 0)
    project(modelViewProjMatrix, outVec, 0)
  }

  private fun project(modelViewProjMatrix: FloatArray, outVec: FloatArray, offset: Int) {
    clipVector[4] = 0.0f
    clipVector[5] = 0.0f
    clipVector[6] = 0.0f
    clipVector[7] = 1.0f
    Matrix.multiplyMV(outVec, offset, modelViewProjMatrix, 0, clipVector, 4)
  }

  /** Sub classes should implement this to actually draw this [SceneObject].  */
  protected open fun drawImpl(mvpMatrix: FloatArray?) {}

  companion object {
    private val log = Log("SceneObject")
  }

  init {
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver)
    this.scene = scene
    widthPx = 1.0f
    heightPx = 1.0f
    Matrix.setIdentityM(matrix, 0)
    Matrix.setIdentityM(modelViewProjMatrix, 0)
  }
}