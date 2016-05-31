package au.com.codeka.warworlds.client.starfield;

import au.com.codeka.warworlds.client.opengl.DimensionResolver;
import au.com.codeka.warworlds.client.opengl.SceneObject;
import au.com.codeka.warworlds.common.Colour;

/**
 * A {@link SceneObject} that we can add to a star or fleet to indicate that it's the currently-
 * selected one.
 */
public class SelectionIndicatorSceneObject extends BaseIndicatorSceneObject {
  public SelectionIndicatorSceneObject(DimensionResolver dimensionResolver) {
    super(dimensionResolver, Colour.WHITE);
  }
}
