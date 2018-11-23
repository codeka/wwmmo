package au.com.codeka.warworlds.client.opengl;

import com.google.common.base.Preconditions;

/** A {@link Sprite} is basically a quad + texture. */
public class Sprite extends SceneObject {
  private final SpriteTemplate tmpl;
  private float alpha;

  public Sprite(DimensionResolver dimensionResolver, SpriteTemplate tmpl) {
    super(dimensionResolver);
    this.alpha = 1.0f;
    this.tmpl = Preconditions.checkNotNull(tmpl);
  }

  public void setAlpha(float alpha) {
    this.alpha = alpha;
  }

  public float getAlpha() {
    return alpha;
  }

  @Override
  protected void drawImpl(float[] mvpMatrix) {
    tmpl.draw(mvpMatrix, alpha);
  }
}
