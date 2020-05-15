package au.com.codeka.warworlds.client.opengl

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import au.com.codeka.warworlds.client.concurrency.RunnableQueue
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.common.Log
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10

fun EGLConfig.findAttribute(egl: EGL10, display: EGLDisplay, attribute: Int, defValue: Int): Int {
  val value = intArrayOf(0);
  if (egl.eglGetConfigAttrib(display, this, attribute, value)) {
    return value[0]
  }
  return defValue
}

fun EGLConfig.debugString(egl: EGL10, display: EGLDisplay): String {
  val rSize = this.findAttribute(egl, display, EGL10.EGL_RED_SIZE, 0)
  val gSize = this.findAttribute(egl, display, EGL10.EGL_GREEN_SIZE, 0)
  val bSize = this.findAttribute(egl, display, EGL10.EGL_BLUE_SIZE, 0)
  val aSize = this.findAttribute(egl, display, EGL10.EGL_ALPHA_SIZE, 0)
  val depthSize = this.findAttribute(egl, display, EGL10.EGL_DEPTH_SIZE, 0)
  val stencilSize = this.findAttribute(egl, display, EGL10.EGL_STENCIL_SIZE, 0)
  val sampleSize = this.findAttribute(egl, display, EGL10.EGL_SAMPLES, 0)
  return "RGBA_$rSize.$gSize.$bSize.$aSize D:$depthSize S:$stencilSize MSAA:${sampleSize}x"
}

/** GLSurfaceView upon which we do all of our rendering.  */
class RenderSurfaceView constructor(context: Context, attrs: AttributeSet? = null)
    : GLSurfaceView(context, attrs) {
  companion object {
    private val log = Log("RenderSurfaceView")

    // We'll prefer multi-sampling. TODO: make this configurable
    private const val PREFER_MULTISAMPLE = true
  }

  private lateinit var renderer: Renderer

  init {
    setEGLContextClientVersion(2)
    setEGLConfigChooser(ConfigChooser())
  }

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
    renderer.setScene(scene)
  }

  /**
   * Creates a new [Scene], which you can populate and then later call @{link #setScene}.
   */
  fun createScene(): Scene {
    return renderer.createScene()
  }

  /** Gets the [Camera] you can use to scroll around the view, etc.  */
  val camera: Camera
    get() {
      return renderer.camera
    }

  /** Gets the [FrameCounter], used to count FPS.  */
  val frameCounter: FrameCounter
    get() {
      return renderer.frameCounter
    }

  class Renderer(context: Context) : GLSurfaceView.Renderer {
    private var deviceInfo: DeviceInfo? = null
    private val dimensionResolver: DimensionResolver = DimensionResolver(context)
    private val textureManager: TextureManager = TextureManager(context)
    private var scene: Scene? = null
    private val runnableQueue: RunnableQueue = RunnableQueue(50 /* numQueuedItems */)
    val camera: Camera = Camera()
    val frameCounter: FrameCounter = FrameCounter()

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
        synchronized(currScene.lock) {
          currScene.draw(camera)
        }
      }
    }
  }

  private class ConfigChooser : EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
      val attributes = intArrayOf(
          EGL10.EGL_RED_SIZE, 8,
          EGL10.EGL_GREEN_SIZE, 8,
          EGL10.EGL_BLUE_SIZE, 8,
          EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
          EGL10.EGL_NONE
      )

      val numConfigsArray = intArrayOf(0)
      if (!egl.eglChooseConfig(display, attributes, null, 0, numConfigsArray)) {
        throw Exception("eglChooseConfig failed")
      }

      val numConfigs = numConfigsArray[0]
      if (numConfigs <= 0) {
        throw Exception("No matching configs found.")
      }

      val configs = arrayOfNulls<EGLConfig>(numConfigs)
      if (!egl.eglChooseConfig(display, attributes, configs, numConfigs, numConfigsArray)) {
        throw Exception("eglChooseConfig failed")
      }

      var bestConfig: EGLConfig? = null
      for (config in configs) {
        if (config == null) {
          continue
        }
        log.debug("- testing config: ${config.debugString(egl, display)}")

        if (config.findAttribute(egl, display, EGL10.EGL_DEPTH_SIZE, 0) != 0) {
          log.debug(" skipping due to non-0 depth size")
          continue
        }
        if (config.findAttribute(egl, display, EGL10.EGL_STENCIL_SIZE, 0) != 0) {
          log.debug(" skipping due to non-0 stencil size")
          continue
        }

        if (bestConfig == null) {
          log.debug(" first viable config")
          bestConfig = config
          continue
        }

        // If we want multi-sampling and this one has multi-sampling, then it's better.
        if (PREFER_MULTISAMPLE &&
            config.findAttribute(egl, display, EGL10.EGL_SAMPLES, 0) >
                bestConfig.findAttribute(egl, display, EGL10.EGL_SAMPLES, 0)) {
          log.debug(" updating best to config which has " +
              "${config.findAttribute(egl, display, EGL10.EGL_SAMPLES, 0)}x multi-sampling")
          bestConfig = config
        }
      }

      if (bestConfig == null) {
        log.info("Couldn't find a config, returning a random: ${configs[0]!!.debugString(egl, display)}")
        return configs[0]!!
      }
      log.info("Found config: ${bestConfig.debugString(egl, display)}")
      return bestConfig
    }
  }
}