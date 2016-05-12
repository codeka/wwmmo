package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;

import com.google.common.base.Preconditions;

/** A {@link Sprite} is basically a quad + texture. */
public class Sprite extends SceneObject {
  private final DimensionResolver dimensionResolver;
  private final SpriteTemplate tmpl;
  private float widthPx;
  private float heightPx;

  public Sprite(DimensionResolver dimensionResolver, SpriteTemplate tmpl) {
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver);
    this.tmpl = Preconditions.checkNotNull(tmpl);
    this.widthPx = 1.0f;
    this.heightPx = 1.0f;
  }

  public void setSizeDp(float widthDp, float heightDp) {
    float widthPx = dimensionResolver.dp2px(widthDp);
    float heightPx = dimensionResolver.dp2px(heightDp);
    Matrix.scaleM(matrix, 0, widthPx / this.widthPx, heightPx / this.heightPx, 1.0f);
    this.widthPx = widthPx;
    this.heightPx = heightPx;
  }

  @Override
  protected void drawImpl(float[] mvpMatrix) {
    tmpl.draw(mvpMatrix);
  }
}
