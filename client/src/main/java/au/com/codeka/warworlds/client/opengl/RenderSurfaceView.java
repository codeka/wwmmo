package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.google.common.base.Preconditions;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import au.com.codeka.warworlds.client.concurrency.TaskQueue;
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

  public static class Renderer implements GLSurfaceView.Renderer {
    private final boolean multiSampling;
    private DeviceInfo deviceInfo;
    private final DimensionResolver dimensionResolver;
    private final TextureManager textureManager;
    @Nullable private Scene scene;
    private TaskQueue taskQueue;
    float[] projMatrix = new float[16];

    public Renderer(Context context) {
      this.multiSampling = true;
      this.textureManager = new TextureManager(context);
      this.taskQueue = new TaskQueue(50 /* numQueuedItems */);
      this.dimensionResolver = new DimensionResolver(context);
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
    public void onSurfaceCreated(final GL10 _, final EGLConfig eglConfig) {
      deviceInfo = new DeviceInfo();
      GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
      GLES20.glEnable(GLES20.GL_BLEND);
      GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void onSurfaceChanged(final GL10 _, final int width, final int height) {
      log.debug("Surface size set to %dx%d", width, height);
      GLES20.glViewport(0, 0, width, height);
      Matrix.orthoM(projMatrix, 0, -width / 2, width / 2, -height / 2, height / 2, 10, -10);
    }

    @Override
    public void onDrawFrame(final GL10 _) {
      Threads.GL.setThread(Thread.currentThread(), taskQueue);

      // Empty the task queue
      taskQueue.runAllTasks();

      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

      Scene currScene = null;
      synchronized (this) {
        currScene = this.scene;
      }
      if (currScene != null) {
        currScene.draw(projMatrix);
      }
    }
  }
}