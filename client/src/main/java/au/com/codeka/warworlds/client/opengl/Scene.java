package au.com.codeka.warworlds.client.opengl;

import com.google.common.base.Preconditions;

/**
 * A {@link Scene} is basically a collection of {@link SceneObject}s that we'll be rendering.
 */
public class Scene {
  private final DimensionResolver dimensionResolver;
  private final TextureManager textureManager;

  private Sprite sprite;

  public Scene(DimensionResolver dimensionResolver, TextureManager textureManager) {
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver);
    this.textureManager = Preconditions.checkNotNull(textureManager);

    sprite = createSprite(new SpriteTemplate.Builder()
        .shader(new SpriteShader())
        .texture(textureManager.loadTexture("stars/stars_small.png"))
        .uvTopLeft(new Vector2(0.25f, 0.5f))
        .uvBottomRight(new Vector2(0.5f, 0.75f))
        .build());
    sprite.setSizeDp(40.0f, 40.0f);
  }

  public Sprite createSprite(SpriteTemplate tmpl) {
    return new Sprite(dimensionResolver, tmpl);
  }

  public void draw(float[] projMatrix) {
    sprite.draw(projMatrix);
  }
}
