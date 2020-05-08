package au.com.codeka.warworlds.client.opengl

import com.google.common.base.Preconditions

/**
 * A [Scene] is basically a collection of [SceneObject]s that we'll be rendering.
 */
class Scene(val dimensionResolver: DimensionResolver, val textureManager: TextureManager) {
  /** Gets the root [SceneObject] that you should add all your sprites and stuff to.  */
  val rootObject = SceneObject(dimensionResolver, "ROOT", this)
  val spriteShader = SpriteShader()
  private val textTexture = TextTexture()

  // All modifications to the scene (adding children, modifying children, etc) should happen while
  // synchronized on this lock.
  val lock = Any()

  fun createSprite(tmpl: SpriteTemplate): Sprite {
    return Sprite(dimensionResolver, tmpl)
  }

  fun createText(text: String): TextSceneObject {
    return TextSceneObject(dimensionResolver, spriteShader, textTexture, text)
  }

  fun draw(camera: Camera) {
    rootObject.draw(camera.viewProjMatrix)
  }
}