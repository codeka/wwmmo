package au.com.codeka.warworlds.client.opengl

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import au.com.codeka.warworlds.client.concurrency.RunnableQueue
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.common.Log
import com.google.common.base.Preconditions
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** GLSurfaceView upon which we do all of our rendering.  */
class RenderSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {
  private var renderer: Renderer? = null

  fun setRenderer() {
    holder.setFormat(PixelFormat.RGBA_8888)
    renderer = Renderer(context)
    setRenderer(renderer)
    renderMode = RENDERMODE_CONTINUOUSLY
  }

  /**
   * Set the [Scene] that we'll be rendering. Can be null, in which case nothing is rendered.
   */
  fun setScene(scene: Scene?) {
    Preconditions.checkState(renderer != null)
    renderer!!.setScene(scene)
  }

  /**
   * Creates a new [Scene], which you can populate and then later call @{link #setScene}.
   */
  fun createScene(): Scene {
    Preconditions.checkState(renderer != null)
    return renderer!!.createScene()
  }

  /** Gets the [Camera] you can use to scroll around the view, etc.  */
  val camera: Camera
    get() {
      Preconditions.checkState(renderer != null)
      return renderer!!.camera
    }

  /** Gets the [FrameCounter], used to count FPS.  */
  val frameCounter: FrameCounter
    get() {
      Preconditions.checkState(renderer != null)
      return renderer!!.frameCounter
    }

  class Renderer(context: Context) : GLSurfaceView.Renderer {
    private val multiSampling = true
    private var deviceInfo: DeviceInfo? = null
    private val dimensionResolver: DimensionResolver
    private val textureManager: TextureManager
    private var scene: Scene? = null
    private val runnableQueue: RunnableQueue
    val camera: Camera
    val frameCounter: FrameCounter
    fun setScene(scene: Scene?) {
      synchronized(this) { this.scene = scene }
    }

    fun createScene(): Scene {
      return Scene(dimensionResolver, textureManager)
    }

    override fun onSurfaceCreated(ignored: GL10, eglConfig: EGLConfig) {
      deviceInfo = DeviceInfo()
      GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
      GLES20.glEnable(GLES20.GL_BLEND)
      GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
      Threads.GL.resetThread()
    }

    override fun onSurfaceChanged(ignored: GL10, width: Int, height: Int) {
      log.debug("Surface size set to %dx%d", width, height)
      GLES20.glViewport(0, 0, width, height)
      camera.onSurfaceChanged(width.toFloat(), height.toFloat())
    }

    override fun onDrawFrame(ignored: GL10) {
      Threads.GL.setThread(Thread.currentThread(), runnableQueue)
      frameCounter.onFrame()

      // Empty the task queue
      runnableQueue.runAllTasks()
      camera.onDraw()
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
      val currScene = scene
      if (currScene != null) {
        synchronized(currScene.lock) { currScene.draw(camera) }
      }
    }

    init {
      textureManager = TextureManager(context)
      runnableQueue = RunnableQueue(50 /* numQueuedItems */)
      dimensionResolver = DimensionResolver(context)
      camera = Camera()
      frameCounter = FrameCounter()
    }
  }

  companion object {
    private val log = Log("RenderSurfaceView")
  }

  init {
    setEGLContextClientVersion(2)
  }
}