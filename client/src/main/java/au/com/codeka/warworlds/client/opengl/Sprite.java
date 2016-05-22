package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;

import com.google.common.base.Preconditions;

/** A {@link Sprite} is basically a quad + texture. */
public class Sprite extends SceneObject {
  private final SpriteTemplate tmpl;

  public Sprite(DimensionResolver dimensionResolver, SpriteTemplate tmpl) {
    super(dimensionResolver);
    this.tmpl = Preconditions.checkNotNull(tmpl);
  }

  @Override
  protected void drawImpl(float[] mvpMatrix) {
    tmpl.draw(mvpMatrix);
  }
}
