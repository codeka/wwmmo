package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import au.com.codeka.warworlds.client.concurrency.RunnableQueue;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.common.Log;

/** GLSurfaceView upon which we do all of our rendering. */
public class RenderSurfaceView extends GLSurfaceView {
  private static Log log = new Log("RenderSurfaceView");
  @Nullable private Renderer renderer;

  public RenderSurfaceView(final Context context) {
    this(context, null);
  }

  public RenderSurfaceView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    setEGLContextClientVersion(2);
  }

  public void setRenderer() {
    getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);

    renderer = new Renderer(getContext());
    setRenderer(renderer);
    setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
  }

  /**
   * Set the {@link Scene} that we'll be rendering. Can be null, in which case nothing is rendered.
   */
  public void setScene(@Nullable Scene scene) {
    Preconditions.checkState(renderer != null);
    renderer.setScene(scene);
  }

  /**
   * Creates a new {@link Scene}, which you can populate and then later call @{link #setScene}.
   */
  public Scene createScene() {
    Preconditions.checkState(renderer != null);
    return renderer.createScene();
  }

  /** Gets the {@link Camera} you can use to scroll around the view, etc. */
  public Camera getCamera() {
    Preconditions.checkState(renderer != null);
    return renderer.camera;
  }

  /** Gets the {@link FrameCounter}, used to count FPS. */
  public FrameCounter getFrameCounter() {
    Preconditions.checkState(renderer != null);
    return renderer.frameCounter;
  }

  public static class Renderer implements GLSurfaceView.Renderer {
    private final boolean multiSampling;
    private DeviceInfo deviceInfo;
    private final DimensionResolver dimensionResolver;
    private final TextureManager textureManager;
    private final Camera camera;
    @Nullable private Scene scene;
    private RunnableQueue runnableQueue;
    private FrameCounter frameCounter;

    public Renderer(Context context) {
      this.multiSampling = true;
      this.textureManager = new TextureManager(context);
      this.runnableQueue = new RunnableQueue(50 /* numQueuedItems */);
      this.dimensionResolver = new DimensionResolver(context);
      this.camera = new Camera();
      this.frameCounter = new FrameCounter();
    }

    public void setScene(@Nullable Scene scene) {
      synchronized (this) {
        this.scene = scene;
      }
    }

    public Scene createScene() {
      return new Scene(dimensionResolver, textureManager);
    }

    @Override
    public void onSurfaceCreated(final GL10 ignored, final EGLConfig eglConfig) {
      deviceInfo = new DeviceInfo();
      GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
      GLES20.glEnable(GLES20.GL_BLEND);
      GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
      Threads.GL.resetThread();
    }

    @Override
    public void onSurfaceChanged(final GL10 ignored, final int width, final int height) {
      log.debug("Surface size set to %dx%d", width, height);
      GLES20.glViewport(0, 0, width, height);
      camera.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(final GL10 ignored) {
      Threads.GL.setThread(Thread.currentThread(), runnableQueue);
      frameCounter.onFrame();

      // Empty the task queue
      runnableQueue.runAllTasks();
      camera.onDraw();

      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

      Scene currScene = this.scene;
      if (currScene != null) {
        synchronized (currScene.lock) {
          currScene.draw(camera);
        }
      }
    }
  }
}
