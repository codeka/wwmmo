package au.com.codeka.warworlds.game.starfield.scene;

import au.com.codeka.common.Colour;
import au.com.codeka.warworlds.opengl.DimensionResolver;
import au.com.codeka.warworlds.opengl.SceneObject;

/**
 * A {@link SceneObject} that we can add to a star or fleet to indicate that it's the currently-
 * selected one.
 */
public class SelectionIndicatorSceneObject extends BaseIndicatorSceneObject {
  public SelectionIndicatorSceneObject(DimensionResolver dimensionResolver) {
    super(dimensionResolver, Colour.WHITE, 5.0f);
  }
}
