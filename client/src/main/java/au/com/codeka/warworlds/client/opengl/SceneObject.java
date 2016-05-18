package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/** Base class for any "object" within a {@link Scene}. */
public class SceneObject {
  /** The scene we belong to, or null if we're not part of a scene. */
  @Nullable private Scene scene;

  /** Children array will be null until you add the first child. */
  @Nullable private ArrayList<SceneObject> children;

  /** Matrix transform that transforms this scene object into world space. */
  protected final float[] matrix = new float[16];
  protected final float[] modelViewProjMatrix = new float[16];

  public SceneObject() {
    this(null);
  }

  public SceneObject(@Nullable Scene scene) {
    this.scene = scene;
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

  public void draw(float[] viewProjMatrix) {
    Matrix.multiplyMM(modelViewProjMatrix, 0, viewProjMatrix, 0, matrix, 0);

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
