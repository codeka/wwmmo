package au.com.codeka.warworlds.client.opengl;

import com.google.common.base.Preconditions;

/**
 * A {@link Scene} is basically a collection of {@link SceneObject}s that we'll be rendering.
 */
public class Scene {
  private final DimensionResolver dimensionResolver;
  private final TextureManager textureManager;
  private final SceneObject rootObject;

  public Scene(DimensionResolver dimensionResolver, TextureManager textureManager) {
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver);
    this.textureManager = Preconditions.checkNotNull(textureManager);
    this.rootObject = new SceneObject();
  }

  public Sprite createSprite(SpriteTemplate tmpl) {
    return new Sprite(dimensionResolver, tmpl);
  }

  public TextureManager getTextureManager() {
    return textureManager;
  }

  /** Gets the root {@link SceneObject} that you should add all your sprites and stuff to. */
  public SceneObject getRootObject() {
    return rootObject;
  }

  public void draw(float[] projMatrix) {
    rootObject.draw(projMatrix);
  }
}
