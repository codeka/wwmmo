package au.com.codeka.warworlds.client.opengl;

import com.google.common.base.Preconditions;

/** A {@link Sprite} is basically a quad + texture. */
public class Sprite extends SceneObject {
  private final SpriteTemplate tmpl;

  public Sprite(SpriteTemplate tmpl) {
    this.tmpl = Preconditions.checkNotNull(tmpl);
  }

  @Override
  protected void drawImpl() {
    tmpl.draw();
  }
}
