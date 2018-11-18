package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;

import com.google.common.base.Preconditions;

import java.util.ArrayList;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;

/** Base class for any "object" within a {@link Scene}. */
public class SceneObject {
  private final static Log log = new Log("SceneObject");
  private final DimensionResolver dimensionResolver;

  /** The scene we belong to, or null if we're not part of a scene. */
  @Nullable
  private Scene scene;

  /** Our parent {@link SceneObject}, if any. */
  @Nullable private SceneObject parent;

  /** Children array will be null until you add the first child. */
  @Nullable private ArrayList<SceneObject> children;

  /** Matrix transform that transforms this scene object into world space. */
  protected final float[] matrix = new float[16];
  protected final float[] modelViewProjMatrix = new float[16];

  /**
   * Temporary 4-component vector used in clipping calculations, must be called inside scene lock.
   */
  private final float[] clipVector = new float[8];

  private float widthPx;
  private float heightPx;
  @Nullable private Float clipRadius;
  @Nullable private Float tapTargetRadius;
  private Object tag;

  /** An optional {@link Runnable} that'll be called before this {@link SceneObject} is drawn. */
  @Nullable private Runnable drawRunnable;

  public SceneObject(DimensionResolver dimensionResolver) {
    this(dimensionResolver, null);
  }

  public SceneObject(DimensionResolver dimensionResolver, @Nullable Scene scene) {
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver);
    this.scene = scene;
    this.widthPx = 1.0f;
    this.heightPx = 1.0f;
    Matrix.setIdentityM(matrix, 0);
    Matrix.setIdentityM(modelViewProjMatrix, 0);
  }

  /**
   * Sets an optional draw {@link Runnable} that is called on the draw thread before this
   * {@link SceneObject} is drawn. You can use this to update the position, scale, etc of the
   * object just before it's drawn (useful for animation, etc).
   *
   * <p>You cannot modify the object heirarchy in this method (add children, remove children, etc)
   */
  public void setDrawRunnable(@Nullable Runnable drawRunnable) {
    this.drawRunnable = drawRunnable;
  }

  @Nullable
  public Scene getScene() {
    return scene;
  }

  @Nullable
  public SceneObject getParent() {
    return parent;
  }

  public void addChild(SceneObject child) {
    if (child.parent != null) {
      child.parent.removeChild(child);
    }
    if (children == null) {
      children = new ArrayList<>();
    }
    children.add(child);
    child.scene = scene;
    child.parent = this;
  }

  public void removeChild(SceneObject child) {
    Preconditions.checkState(child.parent == this, "%s != %s", child.parent, this);
    if (children != null) {
      children.remove(child);
      child.scene = null;
      child.parent = null;
    }
  }

  public void removeAllChildren() {
    if (children != null) {
      children.clear();
    }
  }

  public int getNumChildren() {
    if (children == null) {
      return 0;
    }
    return children.size();
  }

  @Nullable
  public SceneObject getChild(int index) {
    if (children == null) {
      return null;
    }
    if (index < 0 || index >= children.size()) {
      return null;
    }
    return children.get(index);
  }

  /**
   * If non-zero, assume this object is the given radius and refrain from drawing if a circle at
   * our center and with the given radius would be off-screen.
   */
  public void setClipRadius(float radius) {
    clipRadius = radius;
  }

  /**
   * Sets the tap-target radius, in dp, of this {@link SceneObject}. If you set this to a non-zero
   * value, then this {@link SceneObject} will be considered a tap target that you can tap on.
   */
  public void setTapTargetRadius(float radius) {
    tapTargetRadius = radius;
  }

  /**
   * Gets the tap target radius, or <code>null</code> if it hasn't been set yet.
   */
  @Nullable
  public Float getTapTargetRadius() {
    return tapTargetRadius;
  }

  /**
   * Sets this {@link SceneObject}'s "tag", which is just an arbitrary object we'll hang onto for
   * you.
   */
  public void setTag(Object o) {
    tag = o;
  }

  /** Gets the "tag" you previously set in {@link #setTag}. */
  public Object getTag() {
    return tag;
  }

  public void setSize(float widthDp, float heightDp) {
    float widthPx = dimensionResolver.dp2px(widthDp);
    float heightPx = dimensionResolver.dp2px(heightDp);
    Matrix.scaleM(matrix, 0, widthPx / this.widthPx, heightPx / this.heightPx, 1.0f);
    this.widthPx = widthPx;
    this.heightPx = heightPx;
  }

  public void setTranslation(float xDp, float yDp) {
    float xPx = dimensionResolver.dp2px(xDp);
    float yPx = dimensionResolver.dp2px(yDp);
    matrix[12] = 0; matrix[13] = 0; matrix[14] = 0; matrix[15] = 1.0f;
    Matrix.translateM(matrix, 0, xPx / widthPx, yPx / heightPx, 0.0f);
  }

  public void translate(float xDp, float yDp) {
    float xPx = dimensionResolver.dp2px(xDp);
    float yPx = dimensionResolver.dp2px(yDp);
    Matrix.translateM(matrix, 0, xPx, yPx, 0.0f);
  }

  public void rotate(float radians, float x, float y, float z) {
    Matrix.rotateM(matrix, 0, (float)(radians * 180.0f / Math.PI), x, y, z);
  }

  public void setRotation(float radians, float x, float y, float z) {
    float tx = matrix[12];
    float ty = matrix[13];
    Matrix.setRotateM(matrix, 0, (float)(radians * 180.0f / Math.PI), x, y, z);
    Matrix.scaleM(matrix, 0, widthPx, heightPx, 1.0f);
    setTranslation(tx, ty);
  }

  public void draw(float[] viewProjMatrix) {
    Runnable localDrawRunnable = drawRunnable;
    if (localDrawRunnable != null) {
      localDrawRunnable.run();
    }

    Matrix.multiplyMM(modelViewProjMatrix, 0, viewProjMatrix, 0, matrix, 0);

    if (clipRadius != null) {
      clipVector[4] = clipRadius;
      clipVector[5] = 0.0f;
      clipVector[6] = 0.0f;
      clipVector[7] = 1.0f;
      Matrix.multiplyMV(clipVector, 0, modelViewProjMatrix, 0, clipVector, 4);
      float transformedRadius = clipVector[0];

      project(modelViewProjMatrix, clipVector, 0);
      transformedRadius -= clipVector[0];
      if (clipVector[0] < -1.0f - transformedRadius || clipVector[0] > 1.0f + transformedRadius
          || clipVector[1] < -1.0f - transformedRadius || clipVector[1] > 1.0f + transformedRadius) {
        // it's outside of the frustum, clip
        return;
      }
    }

    drawImpl(modelViewProjMatrix);
    if (children != null) {
      for (int i = 0; i < children.size(); i++) {
        children.get(i).draw(modelViewProjMatrix);
      }
    }
  }

  /**
   * Projects this {@link SceneObject}, given the view-proj matrix and returns the clip-space
   * coords in <code>outVec</code>.
   */
  public void project(float[] viewProjMatrix, float[] outVec) {
    Matrix.multiplyMM(modelViewProjMatrix, 0, viewProjMatrix, 0, matrix, 0);
    project(modelViewProjMatrix, outVec, 0);
  }

  private void project(float[] modelViewProjMatrix, float[] outVec, int offset) {
    clipVector[4] = 0.0f;
    clipVector[5] = 0.0f;
    clipVector[6] = 0.0f;
    clipVector[7] = 1.0f;
    Matrix.multiplyMV(outVec, offset, modelViewProjMatrix, 0, clipVector, 4);
  }

  /** Sub classes should implement this to actually draw this {@link SceneObject}. */
  protected void drawImpl(float[] mvpMatrix) {
  }
}
