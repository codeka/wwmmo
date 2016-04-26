package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

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
/*
  public ConfigChooser getConfigChooser() throws IllegalStateException {
    if (this.mConfigChooser == null) {
      throw new IllegalStateException(ConfigChooser.class.getSimpleName() + " not yet set.");
    }
    return this.mConfigChooser;
  }
*/

/*
  @Override
  protected void onMeasure(final int pWidthMeasureSpec, final int pHeightMeasureSpec) {
    if (isInEditMode()) {
      super.onMeasure(pWidthMeasureSpec, pHeightMeasureSpec);
      return;
    }
    this.mEngineRenderer.mEngine.getEngineOptions().getResolutionPolicy().onMeasure(this, pWidthMeasureSpec, pHeightMeasureSpec);
  }
*//*
  @Override
  public void onResolutionChanged(final int pWidth, final int pHeight) {
    this.setMeasuredDimension(pWidth, pHeight);
  }
*/

  public void setRenderer() {
    getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);

    renderer = new Renderer();
    setRenderer(renderer);
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  public static class Renderer implements GLSurfaceView.Renderer {
    private final boolean multiSampling;
    private DeviceInfo deviceInfo;

    public Renderer() {
      this.multiSampling = true;
    }

    @Override
    public void onSurfaceCreated(final GL10 pGL, final EGLConfig pEGLConfig) {
      deviceInfo = new DeviceInfo();
    }

    @Override
    public void onSurfaceChanged(final GL10 pGL, final int pWidth, final int pHeight) {
    }

    @Override
    public void onDrawFrame(final GL10 pGL) {
    }
  }
}