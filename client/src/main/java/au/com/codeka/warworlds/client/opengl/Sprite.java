package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;

import com.google.common.base.Preconditions;

/** A {@link Sprite} is basically a quad + texture. */
public class Sprite extends SceneObject {
  private final DimensionResolver dimensionResolver;
  private final SpriteTemplate tmpl;
  private float width;
  private float height;

  public Sprite(DimensionResolver dimensionResolver, SpriteTemplate tmpl) {
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver);
    this.tmpl = Preconditions.checkNotNull(tmpl);
    this.width = 1.0f;
    this.height = 1.0f;
  }

  public void setSizeDp(float widthDp, float heightDp) {
    float widthPx = dimensionResolver.dp2px(widthDp);
    float heightPx = dimensionResolver.dp2px(heightDp);
    Matrix.scaleM(matrix, 0, widthPx / this.width, heightPx / this.height, 1.0f);
    this.width = widthPx;
    this.height = heightPx;
  }

  @Override
  protected void drawImpl(float[] mvpMatrix) {
    tmpl.draw(mvpMatrix);
  }
}
