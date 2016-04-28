package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/** Base class for any "object" within a {@link Scene}. */
public class SceneObject {
  /** Children array will be null until you add the first child. */
  @Nullable private ArrayList<SceneObject> children;

  /** Matrix transform that transforms this scene object into world space. */
  protected final float[] matrix = new float[16];

  public SceneObject() {
    Matrix.setIdentityM(matrix, 0);
  }

  public void addChild(SceneObject child) {
    if (children == null) {
      children = new ArrayList<>();
    }
    children.add(child);
  }

  public void removeChild(SceneObject child) {
    if (children != null) {
      children.remove(child);
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
    float[] result = new float[16]; // TODO: don't allocate memory
    Matrix.multiplyMM(result, 0, viewProjMatrix, 0, matrix, 0);

    drawImpl(result);
    if (children != null) {
      for (SceneObject child : children) {
        child.draw(result);
      }
    }
  }

  /** Sub classes should implement this to actually draw this {@link SceneObject}. */
  protected void drawImpl(float[] mvpMatrix) {
  }
}
