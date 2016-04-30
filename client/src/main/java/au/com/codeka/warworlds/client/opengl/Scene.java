package au.com.codeka.warworlds.client.opengl;

import com.google.common.base.Preconditions;

/**
 * A {@link Scene} is basically a collection of {@link SceneObject}s that we'll be rendering.
 */
public class Scene {
  private final TextureManager textureManager;

  private Sprite sprite;

  public Scene(TextureManager textureManager) {
    this.textureManager = Preconditions.checkNotNull(textureManager);

    sprite = new Sprite(new SpriteTemplate.Builder()
        .shader(new SpriteShader())
        .texture(textureManager.loadTexture("stars/stars_small.png"))
        .uvTopLeft(new Vector2(0.25f, 0.5f))
        .uvBottomRight(new Vector2(0.5f, 0.75f))
        .build());
  }

  public void draw(float[] projMatrix) {
    sprite.draw(projMatrix);
  }
}
