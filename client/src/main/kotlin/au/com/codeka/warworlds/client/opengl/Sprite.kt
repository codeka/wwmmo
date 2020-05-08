package au.com.codeka.warworlds.client.opengl

/** A [Sprite] is basically a quad + texture.  */
class Sprite(
    dimensionResolver: DimensionResolver, debugName: String, private val tmpl: SpriteTemplate) :
    SceneObject(dimensionResolver, "Sprite:$debugName") {
  var alpha = 1.0f

  override fun drawImpl(mvpMatrix: FloatArray?) {
    tmpl.draw(mvpMatrix, alpha)
  }
}
