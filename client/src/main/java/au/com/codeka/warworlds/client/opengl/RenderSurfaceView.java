package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.google.common.base.Preconditions;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** GLSurfaceView upon which we do all of our rendering. */
public class RenderSurfaceView extends GLSurfaceView {
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
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  public static class Renderer implements GLSurfaceView.Renderer {
    private final boolean multiSampling;
    private DeviceInfo deviceInfo;
    private final TextureManager textureManager;

    private Sprite sprite;

    public Renderer(Context context) {
      this.multiSampling = true;
      this.textureManager = new TextureManager(context);
    }

    @Override
    public void onSurfaceCreated(final GL10 _, final EGLConfig eglConfig) {
      deviceInfo = new DeviceInfo();
      GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
      GLES20.glEnable(GLES20.GL_BLEND);
      GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

      sprite = new Sprite(new SpriteTemplate.Builder()
          .shader(new SpriteShader())
          .texture(textureManager.loadTexture("stars/stars_small.png"))
          .uvTopLeft(new Vector2(0.25f, 0.5f))
          .uvBottomRight(new Vector2(0.5f, 0.75f))
          .build());
    }

    @Override
    public void onSurfaceChanged(final GL10 _, final int width, final int height) {
      GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(final GL10 _) {
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
      sprite.draw();
    }
  }
}