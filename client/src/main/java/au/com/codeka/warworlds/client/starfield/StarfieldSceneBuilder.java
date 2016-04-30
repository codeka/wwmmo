package au.com.codeka.warworlds.client.starfield;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.opengl.Scene;

/**
 * Allows you to build a {@link Scene} and populate it with the properties which control what part
 * of the starfield you're viewing and what extra things you want there.
 */
public class StarfieldSceneBuilder {
  private final RenderSurfaceView renderSurfaceView;

  public StarfieldSceneBuilder(RenderSurfaceView renderSurfaceView) {
    this.renderSurfaceView = Preconditions.checkNotNull(renderSurfaceView);
  }

  public Scene build() {
    return renderSurfaceView.createScene();
  }
}
