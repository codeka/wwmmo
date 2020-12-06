package au.com.codeka.warworlds.opengl;

import com.google.common.base.Preconditions;

/**
 * A {@link Scene} is basically a collection of {@link SceneObject}s that we'll be rendering.
 */
public class Scene {
  private final DimensionResolver dimensionResolver;
  private final TextureManager textureManager;
  private final SceneObject rootObject;

  private final SpriteShader spriteShader;
  private final TextTexture textTexture;

  // All modifications to the scene (adding children, modifying children, etc) should happen while
  // synchronized on this lock.
  public final Object lock = new Object();

  public Scene(DimensionResolver dimensionResolver, TextureManager textureManager) {
    this.dimensionResolver = Preconditions.checkNotNull(dimensionResolver);
    this.textureManager = Preconditions.checkNotNull(textureManager);
    this.rootObject = new SceneObject(dimensionResolver, this);
    this.spriteShader = new SpriteShader();
    this.textTexture = new TextTexture();
  }

  public SpriteShader getSpriteShader() {
    return spriteShader;
  }

  public DimensionResolver getDimensionResolver() {
    return dimensionResolver;
  }

  public Sprite createSprite(SpriteTemplate tmpl) {
    return new Sprite(dimensionResolver, tmpl);
  }

  public TextSceneObject createText(String text) {
    return new TextSceneObject(dimensionResolver, spriteShader, textTexture, text);
  }

  public TextureManager getTextureManager() {
    return textureManager;
  }

  /** Gets the root {@link SceneObject} that you should add all your sprites and stuff to. */
  public SceneObject getRootObject() {
    return rootObject;
  }

  public void draw(Camera camera) {
    rootObject.draw(camera.getViewProjMatrix());
  }
}
