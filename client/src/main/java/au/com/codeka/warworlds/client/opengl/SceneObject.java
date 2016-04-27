package au.com.codeka.warworlds.client.opengl;

import android.support.annotation.Nullable;

import java.util.ArrayList;

/** Base class for any "object" within a {@link Scene}. */
public class SceneObject {
  /** Children array will be null until you add the first child. */
  @Nullable private ArrayList<SceneObject> children;

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

  public void draw() {
    drawImpl();
    if (children != null) {
      for (SceneObject child : children) {
        child.draw();
      }
    }
  }

  /** Sub classes should implement this to actually draw this {@link SceneObject}. */
  protected void drawImpl() {
  }
}
