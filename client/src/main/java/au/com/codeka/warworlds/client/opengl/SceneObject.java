package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.Log;

/** Base class for any "object" within a {@link Scene}. */
public class SceneObject {
  private final static Log log = new Log("SceneObject");
  private final DimensionResolver dimensionResolver;

  /** The scene we belong to, or null if we're not part of a scene. */
  @Nullable private Scene scene;

  /** Children array will be null until you add the first child. */
  @Nullable private ArrayList<SceneObject> children;

  /** Matrix transform that transforms this scene object into world space. */
  protected final float[] matrix = new float[16];
  protected final float[] modelViewProjMatrix = new float[16];

  /** Temporary 4-component vector. */
  private final float[] vec8 = new float[8];

  private float widthPx;
  private float heightPx;
  private float clipRadius;

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

  public void addChild(SceneObject child) {
    if (children == null) {
      children = new ArrayList<>();
    }
    children.add(child);
    child.scene = scene;
  }

  public void removeChild(SceneObject child) {
    if (children != null) {
      children.remove(child);
      child.scene = null;
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

  public void setSize(float widthDp, float heightDp) {
    float widthPx = dimensionResolver.dp2px(widthDp);
    float heightPx = dimensionResolver.dp2px(heightDp);
    Matrix.scaleM(matrix, 0, widthPx / this.widthPx, heightPx / this.heightPx, 1.0f);
    this.widthPx = widthPx;
    this.heightPx = heightPx;
  }

  public void translate(float xDp, float yDp) {
    float xPx = dimensionResolver.dp2px(xDp);
    float yPx = dimensionResolver.dp2px(yDp);
    Matrix.translateM(matrix, 0, xPx, yPx, 0.0f);
  }

  public void draw(float[] viewProjMatrix) {
    Matrix.multiplyMM(modelViewProjMatrix, 0, viewProjMatrix, 0, matrix, 0);

    if (clipRadius > 0.0f) {
      vec8[4] = clipRadius;
      vec8[5] = 0.0f;
      vec8[6] = 0.0f;
      vec8[7] = 1.0f;
      Matrix.multiplyMV(vec8, 0, modelViewProjMatrix, 0, vec8, 4);
      float transformedRadius = vec8[0];

      vec8[4] = 0.0f;
      vec8[5] = 0.0f;
      vec8[6] = 0.0f;
      vec8[7] = 1.0f;
      Matrix.multiplyMV(vec8, 0, modelViewProjMatrix, 0, vec8, 4);
      transformedRadius -= vec8[0];
      if (vec8[0] < -1.0f - transformedRadius || vec8[0] > 1.0f + transformedRadius
          || vec8[1] < -1.0f - transformedRadius || vec8[1] > 1.0f + transformedRadius) {
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

  /** Sub classes should implement this to actually draw this {@link SceneObject}. */
  protected void drawImpl(float[] mvpMatrix) {
  }
}
